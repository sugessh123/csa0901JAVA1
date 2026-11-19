import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Q5 Student B - time 10,000 booking attempts (1,000 seats, 8 threads). */
public class SyncBench {
    interface F { Booker make(int seats); }
    static double timeOnce(Booker b, int attempts, int threads, int seats) throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        Thread[] ts = new Thread[threads];
        AtomicInteger wins = new AtomicInteger();
        for (int t = 0; t < threads; t++) {
            final int id = t;
            ts[t] = new Thread(() -> {
                Random rnd = new Random(id);
                try { go.await(); } catch (InterruptedException e) { return; }
                for (int i = 0; i < attempts / threads; i++) if (b.book(rnd.nextInt(seats))) wins.incrementAndGet();
            });
            ts[t].start();
        }
        long t0 = System.nanoTime(); go.countDown();
        for (Thread t : ts) t.join();
        double ms = (System.nanoTime() - t0) / 1e6;
        if (wins.get() > seats) throw new IllegalStateException("double booking in " + b.name());
        return ms;
    }
    static double median(F f, int seats, int attempts, int threads) throws Exception {
        double[] v = new double[5];
        for (int i = 0; i < 2; i++) timeOnce(f.make(seats), attempts, threads, seats);   // warm-up
        for (int i = 0; i < 5; i++) v[i] = timeOnce(f.make(seats), attempts, threads, seats);
        Arrays.sort(v); return v[2];
    }
    public static void main(String[] a) throws Exception {
        int attempts = 10_000, threads = 8, seats = 1000;
        String[] names = {"synchronized method", "synchronized block", "ReentrantLock", "AtomicBoolean[] CAS", "ConcurrentHashMap.putIfAbsent"};
        F[] fs = { s -> new SyncMethod(s, false), s -> new SyncBlock(s, false), s -> new LockBooker(s), s -> new AtomicBooker(s), s -> new MapBooker() };
        long[] preps = {0, 50_000};
        System.out.println("10,000 booking attempts, 8 threads, 1,000 seats; median of 5 runs (ms)");
        System.out.printf("%-32s %18s %22s%n", "strategy", "prep=0 (pure lock)", "prep=50us outside CS*");
        double[][] res = new double[names.length][2];
        for (int p = 0; p < preps.length; p++) {
            Prep.nanos = preps[p];
            for (int i = 0; i < fs.length; i++) res[i][p] = median(fs[i], seats, attempts, threads);
        }
        for (int i = 0; i < names.length; i++) System.out.printf("%-32s %18.1f %22.1f%n", names[i], res[i][0], res[i][1]);
        System.out.println("* 'prep' = simulated payment validation that does not touch shared seat data. The synchronized METHOD must hold the lock during it.");
    }
}
