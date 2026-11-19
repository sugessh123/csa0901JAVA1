import java.util.concurrent.CountDownLatch;
/** Q5 Student B - why static synchronization is needed once a counter is shared by ALL screens. */
public class StaticSyncDemo {
    static int totalSoldInstanceLock, totalSoldClassLock;
    static class Screen {
        final boolean[] seats = new boolean[100_000];
        synchronized void bookInstance(int s) { if (!seats[s]) { seats[s] = true; bump1(); } }   // lock = this (one per screen)
        synchronized void bookClass(int s)    { if (!seats[s]) { seats[s] = true; bump2(); } }
        void bump1() { int t = totalSoldInstanceLock; Thread.yield(); totalSoldInstanceLock = t + 1; }   // unprotected shared static
        static synchronized void bumpSafe() { int t = totalSoldClassLock; Thread.yield(); totalSoldClassLock = t + 1; }
        void bump2() { bumpSafe(); }                                                                     // lock = StaticSyncDemo.Screen.class
    }
    static void run(boolean classLock) throws Exception {
        int screens = 20, per = 500;
        Screen[] sc = new Screen[screens];
        Thread[] ts = new Thread[screens];
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < screens; i++) {
            final Screen s = sc[i] = new Screen();
            ts[i] = new Thread(() -> { try { go.await(); } catch (InterruptedException e) { return; }
                for (int k = 0; k < per; k++) { if (classLock) s.bookClass(k); else s.bookInstance(k); } });
            ts[i].start();
        }
        go.countDown();
        for (Thread t : ts) t.join();
        System.out.println((classLock ? "static (class) lock on shared counter : " : "instance lock only (per screen)      : ")
                + "total = " + (classLock ? totalSoldClassLock : totalSoldInstanceLock) + " (expected " + screens * per + ")");
    }
    public static void main(String[] a) throws Exception {
        run(false); run(true);
        Object o = new Object();
        Screen s = new Screen();
        synchronized (s) { System.out.println("holdsLock(screen) inside synchronized(this)  = " + Thread.holdsLock(s)); }
        synchronized (Screen.class) { System.out.println("holdsLock(Screen.class) inside class lock       = " + Thread.holdsLock(Screen.class)); }
        System.out.println("holdsLock(screen) outside any lock              = " + Thread.holdsLock(s));
    }
}
