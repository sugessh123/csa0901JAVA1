# Q1 – Handling failure in a Bank ATM simulation

```bash
javac AtmWithdrawal.java UncaughtDemo.java ErrorCodeAtm.java
java AtmWithdrawal          # 7 test inputs, shows which catch ran and that finally ran every time
java UncaughtDemo           # an uncaught NumberFormatException: read the stack trace (Student B)
javac -d /tmp/brk broken/BrokenCatchOrder.java   # EXPECTED to fail: broader catch before narrower
java ErrorCodeAtm           # alternative without exceptions + cost of throw vs return code
```

| File | Purpose |
|---|---|
| `AtmWithdrawal.java` | The withdrawal with try / catch / multi-catch / finally; user-defined `InsufficientFundsException` (checked) and `InvalidAmountException` (unchecked) |
| `UncaughtDemo.java` | Produces a real stack trace to explain line by line |
| `broken/BrokenCatchOrder.java` | Real compiler error "exception FileNotFoundException has already been caught" |
| `ErrorCodeAtm.java` | Error-code design (hasNext checks) and a benchmark of failure cost |

Key result: all 7 test inputs ran exactly one catch block and `finally` ran 7 of 7 times. See `output/`.

Java detail worth knowing for the viva: `Files.newBufferedReader` on a missing file throws `NoSuchFileException`, **not** `FileNotFoundException` (that one comes from `FileReader`/`FileInputStream`). Both extend `IOException`.
