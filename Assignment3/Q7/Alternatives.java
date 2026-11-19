import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Q7 alternatives, each run in its own JVM:  java Alternatives <mode>
 *  modes: executor8 | stream-nocatch | stream-catch | thread-per-file | thread-per-file-2s */
public class Alternatives {
    static String vm(String key) {
        try { for (String l : Files.readAllLines(Path.of("/proc/self/status"))) if (l.startsWith(key)) return l.replaceAll("\\s+", " "); }
        catch (IOException e) { }
        return key + " n/a";
    }
    static class Wrapped extends RuntimeException { Wrapped(Throwable t) { super(t); } }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        int n = 10_000; long latency = 1;
        Path dir = Path.of("/tmp/scores10k_alt");
        if (!Files.exists(dir)) DataGen.generate(dir, n);
        List<Path> files = new ArrayList<>();
        for (int i = 1; i <= n; i++) files.add(dir.resolve(String.format("score%05d.txt", i)));
        EnumMap<Status, Integer> exp = DataGen.expected(n);
        System.out.println("mode = " + mode + ", files = " + n + ", simulated latency = " + (mode.endsWith("2s") ? 2000 : latency) + " ms");
        long t0 = System.nanoTime();
        ConcurrentMap<String, ScoreResult> results = new ConcurrentHashMap<>();
        ConcurrentHashMap<Status, AtomicInteger> counts = new ConcurrentHashMap<>();
        for (Status s : Status.values()) counts.put(s, new AtomicInteger());
        AtomicInteger processed = new AtomicInteger();

        switch (mode) {
            case "executor8" -> {
                Summary s = ResultProcessor.process(files, 8, latency, 30_000);
                System.out.println("summary: " + s.render());
            }
            case "stream-nocatch" -> {
                try {
                    files.parallelStream().forEach(f -> {
                        try { new ScoreTask(f, results, latency).call(); processed.incrementAndGet(); }
                        catch (Exception e) { throw new Wrapped(e); }       // checked exceptions cannot cross the lambda
                    });
                    System.out.println("stream finished normally");
                } catch (Wrapped w) {
                    System.out.println("STREAM ABORTED by first failure: " + w.getCause());
                    System.out.println("files processed before the abort: " + processed.get() + " of " + n + "  -> NO summary of the rest");
                }
            }
            case "stream-catch" -> {
                files.parallelStream().forEach(f -> {
                    try { new ScoreTask(f, results, latency).call(); counts.get(Status.OK).incrementAndGet(); }
                    catch (ScoreFileException e) { counts.get(e.status).incrementAndGet(); }
                    catch (IOException e) { counts.get(Status.IO_ERROR).incrementAndGet(); }
                    catch (Exception e) { counts.get(Status.UNEXPECTED).incrementAndGet(); }
                });
                StringBuilder sb = new StringBuilder();
                boolean ok = true;
                for (Status s : Status.values()) { if (counts.get(s).get() > 0) sb.append(s).append('=').append(counts.get(s).get()).append(' '); if (counts.get(s).get() != exp.get(s)) ok = false; }
                System.out.println("counts: " + sb + "| match expected: " + ok);
                System.out.println("commonPool parallelism = " + ForkJoinPool.commonPool().getParallelism());
            }
            case "thread-per-file", "thread-per-file-2s" -> {
                long lat = mode.endsWith("2s") ? 2000 : latency;
                List<Thread> ts = new ArrayList<>(n);
                try {
                    for (Path f : files) {
                        Thread t = new Thread(() -> {
                            try { new ScoreTask(f, results, lat).call(); counts.get(Status.OK).incrementAndGet(); }
                            catch (ScoreFileException e) { counts.get(e.status).incrementAndGet(); }
                            catch (Exception e) { counts.get(Status.UNEXPECTED).incrementAndGet(); }
                        });
                        t.start(); ts.add(t);
                    }
                } catch (Throwable e) { System.out.println("THREAD CREATION FAILED after " + ts.size() + " threads: " + e); }
                for (Thread t : ts) t.join();
                StringBuilder sb = new StringBuilder(); boolean ok = true;
                for (Status s : Status.values()) { if (counts.get(s).get() > 0) sb.append(s).append('=').append(counts.get(s).get()).append(' '); if (counts.get(s).get() != exp.get(s)) ok = false; }
                System.out.println("counts: " + sb + "| match expected: " + ok);
            }
            default -> System.out.println("unknown mode");
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        System.out.printf("elapsed = %.0f ms | peak live threads = %d | %s | %s%n", ms,
                ManagementFactory.getThreadMXBean().getPeakThreadCount(), vm("VmHWM"), vm("VmRSS"));
    }
}
