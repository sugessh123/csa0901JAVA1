import java.util.concurrent.*;
/** Q4 alternative: fixed thread pool vs one raw Thread per station. n scripts, p threads. */
public class PoolCompare {
    static void scan() { try { Thread.sleep(3); } catch (InterruptedException e) {} }

    static double pool(int n, int p) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(p);
        long t0 = System.nanoTime();
        Future<?>[] fs = new Future<?>[n];
        for (int i = 0; i < n; i++) fs[i] = ex.submit(PoolCompare::scan);
        for (Future<?> f : fs) f.get();
        double ms = (System.nanoTime() - t0) / 1e6;
        ex.shutdown(); return ms;
    }
    static double rawThreads(int n, int p) throws Exception {         // p raw threads, each takes n/p scripts
        long t0 = System.nanoTime();
        Thread[] ts = new Thread[p];
        for (int i = 0; i < p; i++) { final int share = n / p; ts[i] = new Thread(() -> { for (int k = 0; k < share; k++) scan(); }); ts[i].start(); }
        for (Thread t : ts) t.join();
        return (System.nanoTime() - t0) / 1e6;
    }
    public static void main(String[] args) throws Exception {
        int n = 400;
        pool(40, 4); rawThreads(40, 4);                                // warm-up
        System.out.println("n = " + n + " scripts, 3 ms each. Ideal time = n/p * 3 ms");
        System.out.printf("%-4s %12s %14s %16s%n", "p", "ideal (ms)", "raw threads", "fixed pool");
        for (int p : new int[]{1, 2, 4, 8}) {
            System.out.printf("%-4d %12.0f %14.1f %16.1f%n", p, (double) n / p * 3, rawThreads(n, p), pool(n, p));
        }
        System.out.println("\nCost of creating threads vs reusing pool workers (1000 tiny tasks, no sleep):");
        long t0 = System.nanoTime();
        Thread[] ts = new Thread[1000];
        for (int i = 0; i < 1000; i++) { ts[i] = new Thread(() -> {}); ts[i].start(); }
        for (Thread t : ts) t.join();
        System.out.printf("  1000 new Thread objects : %.1f ms%n", (System.nanoTime() - t0) / 1e6);
        ExecutorService ex = Executors.newFixedThreadPool(4);
        t0 = System.nanoTime();
        Future<?>[] fs = new Future<?>[1000];
        for (int i = 0; i < 1000; i++) fs[i] = ex.submit(() -> {});
        for (Future<?> f : fs) f.get();
        System.out.printf("  1000 tasks on 4 workers : %.1f ms%n", (System.nanoTime() - t0) / 1e6);
        ex.shutdown();
    }
}
