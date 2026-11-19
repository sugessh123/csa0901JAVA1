import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

interface Booker { boolean book(int seat); String name(); }

/** Shared helper: simulated payment/validation work done for every booking attempt. */
class Prep {
    static long nanos = 0;
    static void work() { if (nanos > 0) LockSupport.parkNanos(nanos); }
}

class Unsafe implements Booker {                       // no synchronization, optional widened window
    final boolean[] booked; int made; final boolean yieldInWindow;
    Unsafe(int s, boolean y) { booked = new boolean[s]; yieldInWindow = y; }
    public boolean book(int seat) {
        Prep.work();
        if (!booked[seat]) {
            if (yieldInWindow) Thread.yield();         // makes the check-then-act gap visible
            booked[seat] = true; made++; return true;
        }
        return false;
    }
    public String name() { return "unsynchronized" + (yieldInWindow ? "+yield" : ""); }
}

class SyncMethod implements Booker {                   // lock = this, whole method (prep inside lock)
    final boolean[] booked; int made; final boolean yieldInWindow;
    SyncMethod(int s, boolean y) { booked = new boolean[s]; yieldInWindow = y; }
    public synchronized boolean book(int seat) {
        Prep.work();
        if (!booked[seat]) { if (yieldInWindow) Thread.yield(); booked[seat] = true; made++; return true; }
        return false;
    }
    public String name() { return "synchronized method"; }
}

class SyncBlock implements Booker {                    // lock = this, only the critical section
    final boolean[] booked; int made; final boolean yieldInWindow;
    SyncBlock(int s, boolean y) { booked = new boolean[s]; yieldInWindow = y; }
    public boolean book(int seat) {
        Prep.work();                                   // outside the lock
        synchronized (this) {
            if (!booked[seat]) { if (yieldInWindow) Thread.yield(); booked[seat] = true; made++; return true; }
            return false;
        }
    }
    public String name() { return "synchronized block"; }
}

class AtomicBooker implements Booker {                 // lock-free: compareAndSet per seat
    final AtomicBoolean[] booked; final AtomicInteger made = new AtomicInteger();
    AtomicBooker(int s) { booked = new AtomicBoolean[s]; for (int i = 0; i < s; i++) booked[i] = new AtomicBoolean(); }
    public boolean book(int seat) {
        Prep.work();
        if (booked[seat].compareAndSet(false, true)) { made.incrementAndGet(); return true; }
        return false;
    }
    public String name() { return "AtomicBoolean[] CAS"; }
}

class LockBooker implements Booker {                   // explicit ReentrantLock
    final boolean[] booked; int made; final ReentrantLock lock = new ReentrantLock();
    LockBooker(int s) { booked = new boolean[s]; }
    public boolean book(int seat) {
        Prep.work();
        lock.lock();
        try { if (!booked[seat]) { booked[seat] = true; made++; return true; } return false; }
        finally { lock.unlock(); }
    }
    public String name() { return "ReentrantLock"; }
}

class MapBooker implements Booker {                    // ConcurrentHashMap.putIfAbsent
    final ConcurrentHashMap<Integer, Boolean> map = new ConcurrentHashMap<>();
    public boolean book(int seat) { Prep.work(); return map.putIfAbsent(seat, Boolean.TRUE) == null; }
    public String name() { return "ConcurrentHashMap.putIfAbsent"; }
}
