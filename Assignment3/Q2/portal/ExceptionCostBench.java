import java.lang.management.ManagementFactory;
/** Q2 alternative analysis: time and heap cost of creating exceptions at different stack depths. */
public class ExceptionCostBench {
    static final com.sun.management.ThreadMXBean TMX = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static class Checked extends Exception { Checked(String m, Throwable c) { super(m, c); } }
    static class Unchecked extends RuntimeException { Unchecked(String m, Throwable c) { super(m, c); } }
    static class NoTrace extends Exception { NoTrace(String m) { super(m, null, false, false); } }
    static Object keep;

    static Object make(int depth, int kind) {
        if (depth > 0) return make(depth - 1, kind);
        switch (kind) {
            case 0: return new Checked("x", null);                         // checked, one trace
            case 1: return new Unchecked("x", null);                       // unchecked, one trace
            case 2: return new Checked("x", new java.io.IOException("io"));// checked + chained cause (two traces)
            default: return new NoTrace("x");                              // no stack trace
        }
    }
    public static void main(String[] a) {
        String[] names = {"checked", "unchecked", "checked+cause", "no-trace"};
        int N = 100_000;
        for (int w = 0; w < 3; w++) for (int k = 0; k < 4; k++) for (int i = 0; i < N; i++) keep = make(50, k);
        System.out.printf("%-8s %-15s %12s %14s%n", "depth", "kind", "ns/create", "bytes/create");
        for (int depth : new int[]{10, 100, 1000}) {
            for (int k = 0; k < 4; k++) {
                int n = depth == 1000 ? 20_000 : N;
                long b0 = TMX.getThreadAllocatedBytes(Thread.currentThread().getId());
                long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) keep = make(depth, k);
                long t1 = System.nanoTime();
                long b1 = TMX.getThreadAllocatedBytes(Thread.currentThread().getId());
                System.out.printf("%-8d %-15s %12d %14d%n", depth, names[k], (t1 - t0) / n, (b1 - b0) / n);
            }
        }
    }
}
