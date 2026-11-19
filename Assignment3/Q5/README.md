# Q5 – Race conditions in a cinema seat-booking system

```bash
# Graded Complete-the-Code program (TODO 1 = the synchronized book() method)
javac -d /tmp/sync complete_code/SeatBooking.java && for i in 1 2 3 4 5 6 7 8 9 10; do java -cp /tmp/sync SeatBooking; done
# The same program WITHOUT synchronized (unsync/ folder) - run it 10 times as the question asks
javac -d /tmp/unsync unsync/SeatBooking.java && for i in 1 2 3 4 5 6 7 8 9 10; do java -cp /tmp/unsync SeatBooking; done

javac Bookers.java RaceTest.java SyncBench.java StaticSyncDemo.java SpaceCost.java
java RaceTest          # reliable reproduction of double booking (latch + widened window)
java SyncBench         # 10,000 bookings: method vs block vs ReentrantLock vs CAS vs ConcurrentHashMap
java StaticSyncDemo    # why a counter shared by all screens needs the class lock
java SpaceCost         # heap bytes per seat for each representation
```

Honest finding to include in your report: the *given* unsynchronized program usually still prints `Seats sold: 10`, because the check-then-book window is only a few nanoseconds and each thread finishes before the next one starts. The bug is real but latent; `RaceTest` makes it appear by releasing all threads at once and widening the window with `Thread.yield()`.
