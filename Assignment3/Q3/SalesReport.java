import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

/** A BufferedReader that counts how many instances are still open (leak detector). */
class TrackedReader extends BufferedReader {
    static final AtomicInteger OPEN = new AtomicInteger();
    static final AtomicInteger OPENED_TOTAL = new AtomicInteger();
    private boolean closed;
    TrackedReader(Path p) throws IOException {
        super(Files.newBufferedReader(p));
        OPEN.incrementAndGet(); OPENED_TOTAL.incrementAndGet();
    }
    @Override public void close() throws IOException {
        if (!closed) { closed = true; OPEN.decrementAndGet(); }
        super.close();
    }
    static void clear() { OPEN.set(0); OPENED_TOTAL.set(0); }
}

class Report {
    final Map<String, Double> perStore = new LinkedHashMap<>();
    final List<String> failed = new ArrayList<>();
    double total() { double t = 0; for (double v : perStore.values()) t += v; return t; }
    String render() {
        StringBuilder sb = new StringBuilder();
        perStore.forEach((k, v) -> sb.append(k).append(',').append(String.format("%.2f", v)).append('\n'));
        for (String f : failed) sb.append("FAILED,").append(f).append('\n');
        sb.append("TOTAL,").append(String.format("%.2f", total())).append('\n');
        return sb.toString();
    }
}

interface ReportGenerator {
    String name();
    Report run(List<Path> stores) throws IOException;
}

public class SalesReport {
    static final Logger LOG = Logger.getLogger("report");
    static {
        LOG.setUseParentHandlers(false);
        StreamHandler h = new StreamHandler(System.out, new java.util.logging.Formatter() {
            @Override public String format(LogRecord r) { return "   LOG " + r.getLevel() + " " + r.getMessage() + "\n"; }
        }) { @Override public synchronized void publish(LogRecord r) { super.publish(r); flush(); } };
        LOG.addHandler(h);
    }

    /** Parses lines of item,qty,price and returns the store total. Throws NumberFormatException on a bad line. */
    static double parseStore(BufferedReader r) throws IOException {
        double sum = 0; String line;
        while ((line = r.readLine()) != null) {
            String[] p = line.split(",");
            sum += Integer.parseInt(p[1].trim()) * Double.parseDouble(p[2].trim());
        }
        return sum;
    }

    /* ---- V0: no closing at all ---- */
    static class NoClose implements ReportGenerator {
        public String name() { return "V0 no close()"; }
        public Report run(List<Path> stores) throws IOException {
            Report rep = new Report();
            for (Path s : stores) {
                try {
                    BufferedReader r = new TrackedReader(s);
                    rep.perStore.put(s.getFileName().toString(), parseStore(r));
                } catch (IOException | NumberFormatException e) {
                    LOG.warning("store failed: " + s.getFileName() + " -> " + e);
                    rep.failed.add(s.getFileName().toString());
                }
            }
            return rep;
        }
    }

    /* ---- V1: manual close() only on the happy path (the retail chain's version) ---- */
    static class ManualHappyPath implements ReportGenerator {
        public String name() { return "V1 manual close() in try"; }
        public Report run(List<Path> stores) throws IOException {
            Report rep = new Report();
            for (Path s : stores) {
                try {
                    BufferedReader r = new TrackedReader(s);
                    double v = parseStore(r);          // <-- an exception here skips the next line
                    r.close();                         // <-- LEAK when parseStore throws
                    rep.perStore.put(s.getFileName().toString(), v);
                } catch (IOException | NumberFormatException e) {
                    LOG.warning("store failed: " + s.getFileName() + " -> " + e);
                    rep.failed.add(s.getFileName().toString());
                }
            }
            return rep;
        }
    }

    /* ---- V2: manual close() in finally (correct but verbose) ---- */
    static class ManualFinally implements ReportGenerator {
        public String name() { return "V2 manual close() in finally"; }
        public Report run(List<Path> stores) throws IOException {
            Report rep = new Report();
            for (Path s : stores) {
                BufferedReader r = null;
                try {
                    r = new TrackedReader(s);
                    rep.perStore.put(s.getFileName().toString(), parseStore(r));
                } catch (IOException | NumberFormatException e) {
                    LOG.warning("store failed: " + s.getFileName() + " -> " + e);
                    rep.failed.add(s.getFileName().toString());
                } finally {
                    if (r != null) {
                        try { r.close(); }
                        catch (IOException ce) { LOG.warning("close failed: " + s.getFileName() + " -> " + ce); }
                    }
                }
            }
            return rep;
        }
    }

    /* ---- V3: try-with-resources ---- */
    static class TryWithResources implements ReportGenerator {
        public String name() { return "V3 try-with-resources"; }
        public Report run(List<Path> stores) throws IOException {
            Report rep = new Report();
            for (Path s : stores) {
                try (BufferedReader r = new TrackedReader(s)) {
                    rep.perStore.put(s.getFileName().toString(), parseStore(r));
                } catch (IOException | NumberFormatException e) {
                    LOG.warning("store failed: " + s.getFileName() + " -> " + e);
                    rep.failed.add(s.getFileName().toString());
                }
            }
            return rep;
        }
    }

    /* ---- test data ---- */
    static List<Path> makeStores(Path dir, int stores, int lines, int corruptEvery) throws IOException {
        List<Path> out = new ArrayList<>();
        for (int i = 1; i <= stores; i++) {
            Path p = dir.resolve(String.format("store%03d.csv", i));
            StringBuilder sb = new StringBuilder();
            boolean bad = corruptEvery > 0 && i % corruptEvery == 0;
            for (int l = 0; l < lines; l++) {
                if (bad && l == lines / 2) sb.append("ITEM").append(l).append(",x,12.5\n");   // corrupt qty, mid-file
                else sb.append("ITEM").append(l).append(',').append(1 + l % 5).append(',').append(10 + l % 7).append(".5\n");
            }
            Files.writeString(p, sb.toString());
            out.add(p);
        }
        return out;
    }
    static int osFds() {
        String[] a = new File("/proc/self/fd").list();
        return a == null ? -1 : a.length;
    }
    static double medianMs(ReportGenerator g, List<Path> st, int reps) throws IOException {
        double[] t = new double[reps];
        for (int i = 0; i < reps; i++) { long t0 = System.nanoTime(); g.run(st); t[i] = (System.nanoTime() - t0) / 1e6; }
        Arrays.sort(t); return t[reps / 2];
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("sales");
        ReportGenerator[] gens = { new NoClose(), new ManualHappyPath(), new ManualFinally(), new TryWithResources() };

        // ---------- Experiment A : same report on good data ----------
        System.out.println("=== A. Same input (40 valid stores), is the report identical? ===");
        List<Path> good = makeStores(dir, 40, 200, 0);
        String ref = null;
        for (ReportGenerator g : gens) {
            TrackedReader.clear();
            String txt = g.run(good).render();
            if (ref == null) ref = txt;
            System.out.printf("%-30s identical=%-5s  total line: %s", g.name(), txt.equals(ref), txt.substring(txt.lastIndexOf("TOTAL")));
        }

        // ---------- Experiment B : leak count on failing input ----------
        System.out.println("\n=== B. Failing input: 50 stores, every 5th corrupt mid-file (10 bad files) ===");
        Path dirB = Files.createTempDirectory("salesB");
        List<Path> bad = makeStores(dirB, 50, 200, 5);
        System.out.printf("%-30s %8s %12s %14s %14s %s%n", "version", "opened", "still open", "OS fd delta", "after GC", "report matches V3");
        String v3 = new TryWithResources().run(bad).render();
        System.out.println("   (V3 log lines above are expected: one WARNING per corrupt store)");
        for (ReportGenerator g : gens) {
            System.gc(); Thread.sleep(150);
            TrackedReader.clear();
            int fd0 = osFds();
            LOG.setLevel(Level.OFF);                         // keep the table readable
            String txt = g.run(bad).render();
            LOG.setLevel(Level.ALL);
            int fd1 = osFds();
            System.gc(); Thread.sleep(300);
            int fd2 = osFds();
            System.out.printf("%-30s %8d %12d %14d %14d %s%n", g.name(), TrackedReader.OPENED_TOTAL.get(), TrackedReader.OPEN.get(),
                    fd1 - fd0, fd2 - fd0, txt.equals(v3));
        }

        // ---------- Experiment C : logging shows which file failed ----------
        System.out.println("\n=== C. Logging output of V3 on the failing input (first 3 shown) ===");
        PrintStream real = System.out;
        ByteArrayOutputStream cap = new ByteArrayOutputStream();
        // re-run with output captured to show only first lines
        System.setOut(new PrintStream(cap));
        for (Handler h : LOG.getHandlers()) LOG.removeHandler(h);
        StreamHandler h2 = new StreamHandler(System.out, new java.util.logging.Formatter() {
            @Override public String format(LogRecord r) { return "   LOG " + r.getLevel() + " " + r.getMessage() + "\n"; }
        }); LOG.addHandler(h2);
        new TryWithResources().run(bad); h2.flush();
        System.setOut(real);
        String[] lines = cap.toString().split("\n");
        for (int i = 0; i < Math.min(3, lines.length); i++) System.out.println(lines[i]);
        System.out.println("   ... (" + lines.length + " warnings in total, one per corrupt store)");

        // ---------- Experiment D : time vs number of stores ----------
        System.out.println("\n=== D. Median processing time (ms, 5 runs) for f valid store files of 200 lines ===");
        for (Handler h : LOG.getHandlers()) LOG.removeHandler(h);
        System.out.printf("%-8s %14s %14s%n", "f", "V2 finally", "V3 TWR");
        for (int f : new int[]{100, 200, 400, 800}) {
            Path d = Files.createTempDirectory("salesD" + f);
            List<Path> st = makeStores(d, f, 200, 0);
            for (int w = 0; w < 3; w++) { new ManualFinally().run(st); new TryWithResources().run(st); }
            System.out.printf("%-8d %14.1f %14.1f%n", f, medianMs(new ManualFinally(), st, 5), medianMs(new TryWithResources(), st, 5));
        }
    }
}
