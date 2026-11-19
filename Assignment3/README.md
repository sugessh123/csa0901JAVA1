# CSA09 – Programming in Java – Assignment 3 (CO3: Exceptions and Multithreading)

| | Student A | Student B |
|---|---|---|
| Name | T.Achyuth | [partner's name] |
| Register number | 192372257 | [RegNo B] |
| GitHub repository | [link A] | [link B] |

**Java version used:** JDK 17 or later (tested with OpenJDK 21). No external libraries.
**Repository name to use:** `CSA09-192372257-Assignment3`.

## Folder map (one folder per question)

| Folder | Topic | Main programs |
|---|---|---|
| `Q1/` | ATM withdrawal: try / multi-catch / finally | `AtmWithdrawal`, `UncaughtDemo`, `broken/BrokenCatchOrder`, `ErrorCodeAtm` |
| `Q2/` | Exam portal: user-defined exceptions, propagation, chaining | `complete_code/StorageDemo` (graded), `portal/ExamPortal`, `portal/ExceptionCostBench` |
| `Q3/` | Resource safety: manual close vs try-with-resources | `SalesReport`, `SuppressedDemo`, `FdExhaustionDemo` |
| `Q4/` | Thread creation, life cycle, pool alternative | `ScanStations`, `ScanOrder` + `run10.sh`, `DaemonDemo`, `PriorityStats`, `PoolCompare` |
| `Q5/` | Race conditions and synchronization | `complete_code/SeatBooking` (graded), `RaceTest`, `SyncBench`, `StaticSyncDemo` |
| `Q6/` | Deadlock and producer–consumer | `partA/Transfers`, `partB/complete_code/TokenCounter` (graded), `partB/Experiments` |
| `Q7/` | Concurrent fault-tolerant result processor | `ResultProcessor`, `HandCheck`, `Measure`, `Alternatives` |
| `figures/` | UML / state diagrams and charts used in the report | |
| `Assignment3_Report.docx` | The joint report (Tasks Q1–Q7, Student A / Student B sections) | |

Every question folder has its own `README.md` (what to run, what you should see) and an `output/` folder with the console output captured when the programs were run.

## How to compile and run everything

```bash
./run_all.sh --quick     # about 3-4 minutes: all programs, shorter repetitions
./run_all.sh             # about 8-10 minutes: includes the long measurements
```

Or run one question by hand, for example Q5:

```bash
cd Q5
javac -d /tmp/sync complete_code/SeatBooking.java && java -cp /tmp/sync SeatBooking
javac Bookers.java RaceTest.java && java RaceTest
```

## Before you submit (important)

1. **Both students must run every question in their own repository** and take their own screenshots of the console output (put them in each `Qn/output/` folder). The `.txt` files here are the output from the machine this solution was prepared on; **your numbers will differ** (especially timings, thread orders and the number of double bookings), because they depend on your CPU, core count and operating system. Re-run the programs and update the numbers in the report with your own.
2. Replace the placeholder names and register numbers (this file, `CONTRIBUTION.md`, the report cover page).
3. Commit progressively: at least one commit per question, made as you actually work through it, for example:
   ```bash
   git add Q1 && git commit -m "Q1: ATM exception handling, tests and stack-trace evidence"
   git add Q2 && git commit -m "Q2: exam portal exception hierarchy, chaining, StorageDemo completed"
   ```
4. Add the repository link to the report cover page and share the repository with the course faculty.
5. Be ready for the viva: either student may be asked to explain or modify any program.

## Notes on the measurements

Timings were taken on a single-core virtual machine. That is fine for the I/O-bound experiments (sleep-based work scales with threads even on one core) but **CPU-bound speed-ups will be larger on your multi-core laptop**. The reports explain this wherever it matters.
