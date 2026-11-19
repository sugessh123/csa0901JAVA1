import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Q5 Student A - reproduces double booking. 100 threads, 10 seats, all released together by a latch. */
public class RaceTest {
    static int[] one(Booker b, int seats, int threads) throws Exception {
        AtomicInteger[] winners = new AtomicInteger[seats];
        for (int i = 0; i < seats; i++) winners[i] = new AtomicInteger();
        CountDownLatch gate = new CountDownLatch(1);
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int seat = i % seats;
            ts[i] = new Thread(() -> {
                try { gate.await(); } catch (InterruptedException e) { return; }
                if (b.book(seat)) winners[seat].incrementAndGet();      // thread believes it got the seat
            });
            ts[i].start();
        }
        gate.countDown();
        for (Thread t : ts) t.join();
        int doubleSeats = 0, totalWins = 0;
        for (AtomicInteger w : winners) { totalWins += w.get(); if (w.get() > 1) doubleSeats++; }
        return new int[]{doubleSeats, totalWins};
    }
    interface Factory { Booker make(); }
    static void experiment(String label, Factory f, int runs) throws Exception {
        int runsWithDouble = 0, maxSold = 0, sumDouble = 0;
        for (int r = 0; r < runs; r++) {
            int[] res = one(f.make(), 10, 100);
            if (res[0] > 0) runsWithDouble++;
            sumDouble += res[0]; maxSold = Math.max(maxSold, res[1]);
        }
        System.out.printf("%-34s runs=%d  runs with double booking=%3d  avg double-booked seats/run=%.2f  max 'sold'=%d (valid max 10)%n",
                label, runs, runsWithDouble, sumDouble / (double) runs, maxSold);
    }
    public static void main(String[] a) throws Exception {
        int runs = 100;
        System.out.println("=== Race reproduction: 100 threads compete for 10 seats, " + runs + " runs each ===");
        experiment("unsynchronized, no latch gap", () -> new Unsafe(10, false), runs);
        experiment("unsynchronized + yield in window", () -> new Unsafe(10, true), runs);
        experiment("synchronized method + yield", () -> new SyncMethod(10, true), runs);
        experiment("synchronized block  + yield", () -> new SyncBlock(10, true), runs);
    }
}
