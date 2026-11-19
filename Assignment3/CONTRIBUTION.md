# Individual Contribution Record

Fill this in honestly with your partner. The tasks inside each question alternate on purpose, so that both students show *design → implementation → analysis → evaluation*. Both students still run and understand everything.

| Question | Student A – primary responsibility | Student B – primary responsibility | Joint |
|---|---|---|---|
| Q1 | Exception design: Throwable hierarchy diagram, failure table, catch-order justification, `finally` guarantee | Testing and tracing: 5+ test inputs, stack trace explained, broad-before-narrow catch experiment | Failure points, classification, ATM implementation, "catch (Exception e)?" answer |
| Q2 | Exception hierarchy diagram, checked/unchecked reasons, propagation path | Scenarios per exception, `getCause()` evidence, lost-cause demonstration, test table | Hierarchy design, `throw`/`throws` across layers, chaining, `StorageDemo` |
| Q3 | Debugging: breakpoints/step-through, failing input, open-file counts, logging | Design and correctness: resource list, why the leak happens, closing order, suppressed exceptions | Manual and try-with-resources versions, "when is TWR not enough?" |
| Q4 | Thread design: Thread vs Runnable, life-cycle diagram, `run()` vs `start()`, `join()` | Behavioural experiments: 10+ runs, priorities, daemon thread, scheduler explanation | Both station versions, "why does the output order vary?" |
| Q5 | Reproducing and explaining the race, critical section | Timing the strategies, method vs block, static synchronization, intrinsic locks | Unsynchronized + synchronized versions, "which strategy for 20 screens?" |
| Q6 | Producer–consumer design, `wait` in a loop, `notify` vs `notifyAll`, counter bounds | Deadlock construction, four conditions, lock ordering | Both parts implemented, "what do deadlock and producer–consumer share?" |
| Q7 | Stage 2 – Design (executor, Callable/Future, concurrent collection, class diagram, pseudocode) | Stage 3 – Implement and measure (fault injection, pool sizes 1/2/4/8, hand-checked set) | Stage 1 – Interpret, Stage 4 – Defend |

Signed: Student A (T.Achyuth, 192372257) ____________   Student B ____________
