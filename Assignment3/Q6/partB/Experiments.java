import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Q6 Part B - while vs if, notify vs notifyAll, size bounds, sleep vs wait, ArrayBlockingQueue alternative. */
public class Experiments {
    /** result: 0 = correct, 1 = wrong sum / bounds violated, 2 = hung */
    static int[] trial(int cap, boolean w, boolean all, int P, int C, int perProducer, long prodDelayMs) throws Exception {
        TokenCounterTraced c = new TokenCounterTraced(cap, w, all, false);
        AtomicLong sum = new AtomicLong();
        List<Thread> ts = new ArrayList<>();
        for (int p = 0; p < P; p++) ts.add(new Thread(() -> {
            try { for (int t = 1; t <= perProducer; t++) { c.put(t); if (prodDelayMs > 0) Thread.sleep(prodDelayMs); } } catch (InterruptedException e) {} }));
        int perConsumer = P * perProducer / C;
        for (int k = 0; k < C; k++) ts.add(new Thread(() -> {
            try { for (int t = 0; t < perConsumer; t++) sum.addAndGet(c.take()); } catch (InterruptedException e) {} }));
        for (Thread t : ts) { t.setDaemon(true); t.start(); }
        long deadline = System.currentTimeMillis() + 4000;
        for (Thread t : ts) t.join(Math.max(1, deadline - System.currentTimeMillis()));
        boolean hung = false; for (Thread t : ts) if (t.isAlive()) hung = true;
        long expected = (long) P * perProducer * (perProducer + 1) / 2;
        boolean boundsOk = c.maxSeen <= cap && c.minSeen >= 0;
        int outcome = hung ? 2 : (sum.get() == expected && boundsOk ? 0 : 1);
        return new int[]{outcome, c.maxSeen, c.minSeen};
    }
    static void row(String label, int cap, boolean w, boolean all, int P, int C, int per, long delay, int trials) throws Exception {
        int ok = 0, wrong = 0, hung = 0, max = 0, min = 0;
        for (int i = 0; i < trials; i++) {
            int[] r = trial(cap, w, all, P, C, per, delay);
            if (r[0] == 0) ok++; else if (r[0] == 1) wrong++; else hung++;
            max = Math.max(max, r[1]); min = Math.min(min, r[2]);
        }
        System.out.printf("%-44s %3d trials: correct=%3d wrong=%3d HUNG=%3d | max size seen=%d min size seen=%d%n", label, trials, ok, wrong, hung, max, min);
    }
    public static void main(String[] a) throws Exception {
        System.out.println("=== B1. while vs if, notify vs notifyAll (capacity 5 unless stated) ===");
        row("while + notifyAll, 1 producer, 2 consumers", 5, true, true, 1, 2, 20, 1, 30);
        row("if    + notifyAll, 1 producer, 2 consumers", 5, false, true, 1, 2, 20, 1, 30);
        row("while + notify,    1 producer, 2 consumers", 5, true, false, 1, 2, 20, 1, 30);
        row("while + notify,    2 producers, 2 consumers, cap 1", 1, true, false, 2, 2, 20, 0, 30);
        row("while + notifyAll, 2 producers, 2 consumers, cap 1", 1, true, true, 2, 2, 20, 0, 30);

        System.out.println("\n=== B2. Counter size over time (while + notifyAll, 1 producer, 2 consumers, producer pauses 1 ms) ===");
        TokenCounterTraced c = new TokenCounterTraced(5, true, true, true);
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= 20; t++) c.put(t); } catch (InterruptedException e) {} });
        Thread s1 = new Thread(() -> { try { for (int k = 0; k < 10; k++) { c.take(); Thread.sleep(3); } } catch (InterruptedException e) {} });
        Thread s2 = new Thread(() -> { try { for (int k = 0; k < 10; k++) { c.take(); Thread.sleep(5); } } catch (InterruptedException e) {} });
        cook.start(); s1.start(); s2.start(); cook.join(); s1.join(); s2.join();
        System.out.println("size after each of the 40 operations: " + c.sizeLog);
        System.out.println("max size = " + c.maxSeen + " (limit 5), min size = " + c.minSeen + " (limit 0)");

        System.out.println("\n=== B3. sleep() vs wait(): how long does a second thread wait to ENTER the monitor? ===");
        Object lock = new Object();
        for (String mode : new String[]{"sleep(400) inside synchronized", "wait(400) inside synchronized"}) {
            CountDownLatch holding = new CountDownLatch(1);
            Thread holder = new Thread(() -> {
                synchronized (lock) {
                    holding.countDown();
                    try { if (mode.startsWith("sleep")) Thread.sleep(400); else lock.wait(400); } catch (InterruptedException e) {}
                }
            });
            holder.start(); holding.await(); Thread.sleep(20);
            long t0 = System.nanoTime();
            synchronized (lock) { }
            System.out.printf("%-34s -> second thread entered the monitor after %4d ms%n", mode, (System.nanoTime() - t0) / 1_000_000);
            holder.join();
        }

        System.out.println("\n=== B4. Throughput: wait/notifyAll counter vs ArrayBlockingQueue(5), 1 producer, 2 consumers, 300,000 tokens ===");
        int N = 300_000;
        for (int rep = 0; rep < 2; rep++) {
            TokenCounterTraced tc = new TokenCounterTraced(5, true, true, false);
            long t0 = System.nanoTime();
            Thread p = new Thread(() -> { try { for (int i = 0; i < N; i++) tc.put(i); } catch (InterruptedException e) {} });
            Thread c1 = new Thread(() -> { try { for (int i = 0; i < N / 2; i++) tc.take(); } catch (InterruptedException e) {} });
            Thread c2 = new Thread(() -> { try { for (int i = 0; i < N / 2; i++) tc.take(); } catch (InterruptedException e) {} });
            p.start(); c1.start(); c2.start(); p.join(); c1.join(); c2.join();
            double ms1 = (System.nanoTime() - t0) / 1e6;
            ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(5);
            t0 = System.nanoTime();
            Thread p2 = new Thread(() -> { try { for (int i = 0; i < N; i++) q.put(i); } catch (InterruptedException e) {} });
            Thread d1 = new Thread(() -> { try { for (int i = 0; i < N / 2; i++) q.take(); } catch (InterruptedException e) {} });
            Thread d2 = new Thread(() -> { try { for (int i = 0; i < N / 2; i++) q.take(); } catch (InterruptedException e) {} });
            p2.start(); d1.start(); d2.start(); p2.join(); d1.join(); d2.join();
            double ms2 = (System.nanoTime() - t0) / 1e6;
            if (rep == 1) System.out.printf("wait/notifyAll: %.0f ms   ArrayBlockingQueue: %.0f ms%n", ms1, ms2);
        }
    }
}
