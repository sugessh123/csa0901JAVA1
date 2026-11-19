import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/* ===================== domain types ===================== */
enum Status { OK, MISSING, CORRUPT, EMPTY, DUPLICATE, IO_ERROR, TIMEOUT, UNEXPECTED }

/** CHECKED: every per-file failure the processor expects and must record, never crash on. */
class ScoreFileException extends Exception {
    final Status status;
    ScoreFileException(Status s, String m) { super(m); status = s; }
    ScoreFileException(Status s, String m, Throwable cause) { super(m, cause); status = s; }
}
class MissingFileException extends ScoreFileException {
    MissingFileException(String file, Throwable cause) { super(Status.MISSING, file + " does not exist", cause); }
}
class CorruptFileException extends ScoreFileException {
    CorruptFileException(String file, String why) { super(Status.CORRUPT, file + ": " + why); }
    CorruptFileException(String file, String why, Throwable cause) { super(Status.CORRUPT, file + ": " + why, cause); }
    CorruptFileException(Status s, String file, String why) { super(s, file + ": " + why); }   // EMPTY
}
class DuplicateStudentException extends ScoreFileException {
    DuplicateStudentException(String id, String file, String firstFile) {
        super(Status.DUPLICATE, file + ": student " + id + " already recorded from " + firstFile);
    }
}

record ScoreResult(String studentId, String file, int total, double percent, char grade) {}

/* ===================== the unit of work ===================== */
class ScoreTask implements Callable<ScoreResult> {
    static final int MAX_BYTES = 1024;                    // a valid file is one short line
    private final Path file;
    private final ConcurrentMap<String, ScoreResult> results;   // shared, written by many workers
    private final long latencyMs;                         // simulated slow storage

    ScoreTask(Path file, ConcurrentMap<String, ScoreResult> results, long latencyMs) {
        this.file = file; this.results = results; this.latencyMs = latencyMs;
    }

    @Override public ScoreResult call() throws ScoreFileException, IOException, InterruptedException {
        String name = file.getFileName().toString();
        if (latencyMs > 0) Thread.sleep(latencyMs);
        if (!Files.isRegularFile(file)) {
            if (!Files.exists(file)) throw new MissingFileException(name, null);
            throw new IOException(name + " is not a regular file");
        }
        byte[] data;
        try {
            if (Files.size(file) > MAX_BYTES) throw new CorruptFileException(name, "file larger than " + MAX_BYTES + " bytes");
            data = Files.readAllBytes(file);
        } catch (NoSuchFileException e) {                 // deleted between the check and the read
            throw new MissingFileException(name, e);
        }
        String text = new String(data, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);        // strip a UTF-8 byte-order mark
        text = text.strip();
        if (text.isEmpty()) throw new CorruptFileException(Status.EMPTY, name, "file is empty");
        if (text.equals("HANG")) Thread.sleep(5000);      // test hook: simulates a read that never returns in time
        if (text.indexOf('\n') >= 0) throw new CorruptFileException(name, "more than one record line");
        String[] f = text.split(",", -1);
        if (f.length != 6) throw new CorruptFileException(name, "expected 6 fields but found " + f.length);
        String id = f[0].trim();
        if (!id.matches("S\\d{5}")) throw new CorruptFileException(name, "bad student id '" + id + "'");
        int total = 0;
        for (int i = 1; i <= 5; i++) {
            int m;
            try { m = Integer.parseInt(f[i].trim()); }
            catch (NumberFormatException e) { throw new CorruptFileException(name, "mark " + i + " is not an integer: '" + f[i].trim() + "'", e); }
            if (m < 0 || m > 100) throw new CorruptFileException(name, "mark " + i + " out of range 0-100: " + m);
            total += m;
        }
        double pct = total / 5.0;                         // double division: 375/5 -> 75.0 (never integer division)
        char grade = pct >= 90 ? 'O' : pct >= 75 ? 'A' : pct >= 60 ? 'B' : pct >= 50 ? 'C' : 'F';
        ScoreResult r = new ScoreResult(id, name, total, pct, grade);
        ScoreResult prev = results.putIfAbsent(id, r);    // atomic: exactly one worker wins per student id
        if (prev != null) throw new DuplicateStudentException(id, name, prev.file());
        return r;
    }
}

/* ===================== summary ===================== */
class Summary {
    final EnumMap<Status, Integer> counts = new EnumMap<>(Status.class);
    final List<String> failures = new ArrayList<>();
    final int submitted;
    final ConcurrentMap<String, ScoreResult> results;
    long elapsedMs;
    Summary(int submitted, ConcurrentMap<String, ScoreResult> results) {
        this.submitted = submitted; this.results = results;
        for (Status s : Status.values()) counts.put(s, 0);
    }
    void add(Status s) { counts.merge(s, 1, Integer::sum); }
    int count(Status s) { return counts.get(s); }
    int accounted() { int t = 0; for (int c : counts.values()) t += c; return t; }
    /** Completeness invariant: every submitted file has exactly one outcome and successes match the shared map. */
    boolean complete() { return accounted() == submitted && count(Status.OK) == results.size(); }
    String render() {
        StringBuilder sb = new StringBuilder("submitted=" + submitted);
        for (Status s : Status.values()) if (counts.get(s) > 0) sb.append("  ").append(s).append('=').append(counts.get(s));
        sb.append("  | accounted=").append(accounted()).append(" complete=").append(complete());
        return sb.toString();
    }
}

/* ===================== the processor ===================== */
class ResultProcessor {
    static Summary process(List<Path> files, int poolSize, long latencyMs, long stallTimeoutMs) throws InterruptedException {
        ConcurrentMap<String, ScoreResult> results = new ConcurrentHashMap<>();
        Summary sum = new Summary(files.size(), results);
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        long t0 = System.nanoTime();
        try {
            ExecutorCompletionService<ScoreResult> ecs = new ExecutorCompletionService<>(pool);
            Map<Future<ScoreResult>, Path> owner = new IdentityHashMap<>();
            for (Path f : files) owner.put(ecs.submit(new ScoreTask(f, results, latencyMs)), f);

            while (!owner.isEmpty()) {
                Future<ScoreResult> done = ecs.poll(stallTimeoutMs, TimeUnit.MILLISECONDS);
                if (done == null) break;                  // nothing finished for stallTimeoutMs: stop waiting
                Path f = owner.remove(done);
                try {
                    done.get();                           // already complete: returns at once or throws
                    sum.add(Status.OK);
                } catch (ExecutionException ee) {         // the task's exception is wrapped; unwrap the cause
                    Throwable cause = ee.getCause();
                    Status st;
                    if (cause instanceof ScoreFileException sfe) st = sfe.status;
                    else if (cause instanceof IOException) st = Status.IO_ERROR;
                    else st = Status.UNEXPECTED;          // RuntimeException or Error: recorded, never rethrown
                    sum.add(st);
                    sum.failures.add(f.getFileName() + " -> " + st + ": " + cause.getMessage());
                } catch (CancellationException ce) {
                    sum.add(Status.UNEXPECTED);
                    sum.failures.add(f.getFileName() + " -> cancelled");
                }
            }
            for (Path f : owner.values()) {               // tasks still outstanding after the stall timeout
                sum.add(Status.TIMEOUT);
                sum.failures.add(f.getFileName() + " -> TIMEOUT: no result within " + stallTimeoutMs + " ms");
            }
        } finally {
            pool.shutdownNow();                           // interrupts stuck workers; summary is already complete
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
        sum.elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        return sum;
    }
}
