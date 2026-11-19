# Q2 – User-defined exceptions and propagation (online examination portal)

```bash
# Graded "Complete the Code" program (TODO 1 completed, nothing else changed)
cd complete_code
javac -d /tmp/q2a StorageDemo.java && java -cp /tmp/q2a StorageDemo
# Variant with the cause NOT passed - answers "what prints in the Cause line?"
javac -d /tmp/q2b StorageDemoNoCause.java && java -cp /tmp/q2b StorageDemoNoCause

# Full three-layer portal and the exception-cost benchmark
cd ../portal
javac ExamPortal.java ExceptionCostBench.java
java ExamPortal
java ExceptionCostBench
```

Expected output of `StorageDemo`:
```
Saved answer01.pdf
Error: Could not store answer02.pdf
Cause: Disk quota exceeded
```
`StorageDemoNoCause` ends with a `NullPointerException`, because `getCause()` returns `null` when no cause was chained.

(The two `StorageDemo*` files must be compiled into different output folders because they declare the same helper classes.)
