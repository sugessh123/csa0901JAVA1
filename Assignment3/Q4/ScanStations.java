import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Q4 - four scanner stations, written both as Thread subclasses and as Runnables. */
class Counter {
    static final List<String> FINISH_ORDER = Collections.synchronizedList(new ArrayList<>());
    static final AtomicInteger TOTAL = new AtomicInteger();
    static void reset() { FINISH_ORDER.clear(); TOTAL.set(0); }
}

class StationThread extends Thread {                       // way 1: extend Thread
    private final int scripts;
    StationThread(String name, int scripts) { super(name); this.scripts = scripts; }
    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(3); } catch (InterruptedException e) { return; }   // 3 ms to scan one script
            Counter.TOTAL.incrementAndGet();
        }
        Counter.FINISH_ORDER.add(getName());
    }
}

class StationTask implements Runnable {                    // way 2: implement Runnable
    private final String name; private final int scripts;
    StationTask(String name, int scripts) { this.name = name; this.scripts = scripts; }
    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(3); } catch (InterruptedException e) { return; }
            Counter.TOTAL.incrementAndGet();
        }
        Counter.FINISH_ORDER.add(name);
    }
}

public class ScanStations {
    static final int[] BATCH = {40, 40, 40, 40};   // equal work: finish order depends only on scheduling

    static void runAll(Thread[] ts) throws InterruptedException {
        for (Thread t : ts) t.start();
        System.out.print("  isAlive right after start(): ");
        for (Thread t : ts) System.out.print(t.getName() + "=" + t.isAlive() + " ");
        System.out.println();
        for (Thread t : ts) t.join();                          // main waits here
        System.out.print("  isAlive after join()      : ");
        for (Thread t : ts) System.out.print(t.getName() + "=" + t.isAlive() + " ");
        System.out.println();
        System.out.println("  finish order = " + Counter.FINISH_ORDER + ", total scanned = " + Counter.TOTAL.get()
                + " (expected " + Arrays.stream(BATCH).sum() + ")");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. extends Thread ===");
        Counter.reset();
        Thread[] a = new Thread[4];
        for (int i = 0; i < 4; i++) a[i] = new StationThread("S" + (i + 1), BATCH[i]);
        runAll(a);

        System.out.println("=== 2. implements Runnable ===");
        Counter.reset();
        Thread[] b = new Thread[4];
        for (int i = 0; i < 4; i++) b[i] = new Thread(new StationTask("S" + (i + 1), BATCH[i]), "S" + (i + 1));
        runAll(b);

        System.out.println("=== 3. run() vs start(): which thread executes the body? ===");
        Runnable who = () -> System.out.println("  body executed by thread: " + Thread.currentThread().getName());
        Thread t = new Thread(who, "worker-1");
        System.out.print("  calling t.run()   -> "); t.run();
        System.out.print("  calling t.start() -> "); t.start(); t.join();
        System.out.println("  calling t.start() a second time:");
        try { t.start(); } catch (IllegalThreadStateException e) { System.out.println("  " + e); }

        System.out.println("=== 4. Thread life-cycle states observed with getState() ===");
        Object lock = new Object();
        Thread sleeper = new Thread(() -> { try { Thread.sleep(300); } catch (InterruptedException e) {} }, "sleeper");
        Thread waiter  = new Thread(() -> { try { sleeper.join(); } catch (InterruptedException e) {} }, "waiter-on-join");
        Thread blocked = new Thread(() -> { synchronized (lock) { } }, "blocked-on-lock");
        Thread spinner = new Thread(() -> { long x = 0; while (!Thread.currentThread().isInterrupted()) x++; }, "spinner");
        System.out.println("  sleeper before start : " + sleeper.getState());
        synchronized (lock) {                                   // main holds the lock so 'blocked' must wait
            sleeper.start(); waiter.start(); blocked.start(); spinner.start();
            Thread.sleep(100);
            System.out.println("  sleeper              : " + sleeper.getState() + "   (Thread.sleep)");
            System.out.println("  waiter-on-join       : " + waiter.getState() + "         (join without timeout)");
            System.out.println("  blocked-on-lock      : " + blocked.getState() + "         (waiting for monitor)");
            System.out.println("  spinner              : " + spinner.getState() + "        (running / ready)");
        }
        spinner.interrupt(); sleeper.join(); waiter.join(); blocked.join(); spinner.join();
        System.out.println("  sleeper after finish : " + sleeper.getState());
    }
}
