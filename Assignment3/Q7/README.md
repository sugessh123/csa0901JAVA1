# Q7 – Concurrent, fault-tolerant semester result processor

```bash
javac ResultProcessor.java DataGen.java HandCheck.java Measure.java Alternatives.java
java HandCheck                 # hand-checked 12-file set + 12 edge cases + timeout test (23 checks)
java Measure                   # 10,000 files, pools of 1..64 threads, 0 ms and 1 ms latency (about 2 minutes)
java Alternatives executor8    # also: stream-catch | stream-nocatch | thread-per-file | thread-per-file-2s
```

Design in one paragraph: each file is a `Callable<ScoreResult>` that may throw a *checked* `ScoreFileException` subclass. A fixed `ExecutorService` runs them; an `ExecutorCompletionService` hands finished futures to the collector in completion order; `ExecutionException` is unwrapped and classified; the shared `ConcurrentHashMap` is written by the workers (`putIfAbsent` detects duplicate students atomically). A stall timeout marks unfinished files `TIMEOUT`. The invariant `accounted == submitted` is checked at the end (`Summary.complete()`).

`DataGen` injects faults deterministically (rule on `i % 100`), so the expected counts for 10,000 files are known without running the processor: OK 9300, MISSING 200, CORRUPT 300, EMPTY 100, DUPLICATE 100.

Known limitation recorded by `HandCheck`: a UTF-16 encoded file is classified `CORRUPT` (the reader assumes UTF-8).
