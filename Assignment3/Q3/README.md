# Q3 – Resource safety, debugging and robust code (daily sales report)

```bash
javac SalesReport.java SuppressedDemo.java FdExhaustionDemo.java
java SalesReport             # experiments A-D (same report, leak counts, logging, timing)
java SuppressedDemo          # closing order, suppressed exceptions, masked exception
( ulimit -n 100; java -Xmx2g FdExhaustionDemo noclose )   # real "Too many open files"
( ulimit -n 100; java FdExhaustionDemo twr )              # same limit, try-with-resources survives
```

Versions of the generator: `V0` no close, `V1` manual close inside `try` (the retail chain's bug), `V2` manual close in `finally`, `V3` try-with-resources. `TrackedReader` counts open readers; `/proc/self/fd` gives the operating-system view (Linux only; prints -1 elsewhere).

**Debugging (Student A):** compile with `javac -g`, set a breakpoint at the `double v = parseStore(r);` line in `ManualHappyPath`, make store 3 corrupt, and step. `output/jdb_session.txt` is a real command-line debugger transcript; do the same in your IDE and take a screenshot of the Variables/Watch panel showing `TrackedReader.OPEN`.
