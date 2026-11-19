# Q4 – Thread creation and life cycle (parallel answer-script scanning)

```bash
javac ScanStations.java ScanOrder.java DaemonDemo.java PoolCompare.java PriorityStats.java
java ScanStations          # extends Thread, implements Runnable, run() vs start(), thread states
./run10.sh                 # 12 separate JVM runs (default priorities) + 12 with priorities
java DaemonDemo            # S4 as a normal thread
java DaemonDemo daemon     # S4 as a daemon thread: main ends and S4 is killed
java PriorityStats         # 100 trials: does priority change who finishes first?
java PoolCompare           # raw threads vs fixed thread pool, n = 400 scripts
```

`ScanStations.BATCH` holds the work per station. With *unequal* batches (40, 42, 38, 41) the order never changed in 24 runs (`output/order_runs_unequal_batches.txt`); with *equal* batches (40 each) it changed in most runs. Keep both observations in your report: the first shows the order is only "random" when the work is close enough for scheduler jitter to matter.
