import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Q6 Part A - hostel wallet transfers: deadlock-prone, lock-ordered, and tryLock versions. */
public class Transfers {
    static class Account {
        final int id; long balance; final ReentrantLock lock = new ReentrantLock();
        Account(int id, long b) { this.id = id; this.balance = b; }
    }

    /** BUGGY: locks 'from' then 'to'. Two opposite transfers deadlock. */
    static void transferDeadlockProne(Account from, Account to, long amt, long pauseMs) {
        synchronized (from) {
            pause(pauseMs);                                 // widens the window so the deadlock is reliable
            synchronized (to) { from.balance -= amt; to.balance += amt; }
        }
    }
    /** FIXED: always lock the account with the smaller id first (global lock ordering). */
    static void transferOrdered(Account from, Account to, long amt) {
        Account first = from.id < to.id ? from : to, second = first == from ? to : from;
        synchronized (first) { synchronized (second) { from.balance -= amt; to.balance += amt; } }
    }
    /** ALTERNATIVE: tryLock with timeout, back off and retry (breaks 'hold and wait'). */
    static int retries;
    static boolean transferTryLock(Account from, Account to, long amt, AtomicInteger retryCount) throws InterruptedException {
        for (int attempt = 0; attempt < 50; attempt++) {
            if (from.lock.tryLock(2, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(2, TimeUnit.MILLISECONDS)) {
                        try { from.balance -= amt; to.balance += amt; return true; }
                        finally { to.lock.unlock(); }
                    }
                } finally { from.lock.unlock(); }
            }
            retryCount.incrementAndGet();
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 4));   // random back-off
        }
        return false;
    }
    static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) {} }

    public static void main(String[] args) throws Exception {
        System.out.println("=== A1. Deadlock-prone transfer: T1: A->B and T2: B->A at the same time ===");
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> transferDeadlockProne(a, b, 100, 200), "T1(A->B)");
        Thread t2 = new Thread(() -> transferDeadlockProne(b, a, 50, 200), "T2(B->A)");
        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        Thread.sleep(800);
        System.out.println("T1 state = " + t1.getState() + ", T2 state = " + t2.getState());
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] ids = mx.findDeadlockedThreads();
        System.out.println("ThreadMXBean.findDeadlockedThreads() -> " + (ids == null ? "none" : ids.length + " threads deadlocked"));
        if (ids != null) for (ThreadInfo ti : mx.getThreadInfo(ids))
            System.out.println("  " + ti.getThreadName() + " is BLOCKED waiting for " + ti.getLockName().replaceAll("@.*", "") + " held by " + ti.getLockOwnerName());
        System.out.println("Four conditions: mutual exclusion=synchronized monitors; hold-and-wait=holds 'from' while waiting for 'to';");
        System.out.println("                 no pre-emption=monitors cannot be taken away; circular wait=T1 waits for T2's lock and T2 for T1's.");

        System.out.println("\n=== A2. Lock ordering by account id: same two transfers cannot deadlock ===");
        Account c = new Account(1, 1000), d = new Account(2, 1000);
        Thread u1 = new Thread(() -> { pause(200); transferOrdered(c, d, 100); }, "U1(C->D)");
        Thread u2 = new Thread(() -> { pause(200); transferOrdered(d, c, 50); }, "U2(D->C)");
        u1.start(); u2.start(); u1.join(2000); u2.join(2000);
        System.out.println("U1 alive=" + u1.isAlive() + ", U2 alive=" + u2.isAlive() + ", balances C=" + c.balance + " D=" + d.balance + " (expected 950 and 1050)");

        System.out.println("\n=== A3. Stress: 8 threads x 50,000 random transfers among 20 accounts ===");
        int N = 20; long perAcct = 1_000_000;
        for (String mode : new String[]{"ordered synchronized", "tryLock+timeout"}) {
            Account[] acc = new Account[N];
            for (int i = 0; i < N; i++) acc[i] = new Account(i, perAcct);
            AtomicInteger retriesCount = new AtomicInteger(), gaveUp = new AtomicInteger();
            Thread[] ts = new Thread[8];
            long t0 = System.nanoTime();
            for (int t = 0; t < 8; t++) {
                final int seed = t;
                ts[t] = new Thread(() -> {
                    Random r = new Random(seed);
                    for (int k = 0; k < 50_000; k++) {
                        int x = r.nextInt(N), y = r.nextInt(N); if (x == y) continue;
                        try {
                            if (mode.startsWith("ordered")) transferOrdered(acc[x], acc[y], 1 + r.nextInt(10));
                            else if (!transferTryLock(acc[x], acc[y], 1 + r.nextInt(10), retriesCount)) gaveUp.incrementAndGet();
                        } catch (InterruptedException e) { return; }
                    }
                });
                ts[t].start();
            }
            boolean finished = true;
            for (Thread t : ts) { t.join(60_000); if (t.isAlive()) finished = false; }
            double ms = (System.nanoTime() - t0) / 1e6;
            long sum = 0; for (Account x : acc) sum += x.balance;
            System.out.printf("%-22s finished=%b time=%8.1f ms  money conserved=%b (sum=%d)  retries=%d gave up=%d%n",
                    mode, finished, ms, sum == perAcct * N, sum, retriesCount.get(), gaveUp.get());
        }
    }
}
