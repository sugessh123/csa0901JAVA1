# Q6 – Deadlock and inter-thread communication

```bash
# Part A – wallet transfers
cd partA && javac Transfers.java && java Transfers

# Part B – graded TokenCounter (TODO 1 and TODO 2 completed with while + notifyAll)
cd ../partB
javac -d /tmp/q6w complete_code/TokenCounter.java && java -cp /tmp/q6w TokenCounter
# 'if' instead of 'while' (the experiment the question asks for), run many times with a time-out
javac -d /tmp/q6i if_variant/TokenCounter.java
for i in $(seq 1 100); do timeout 5 java -cp /tmp/q6i TokenCounter; done | sort | uniq -c
# Experiments: while/if, notify/notifyAll, counter bounds, sleep vs wait, ArrayBlockingQueue
javac TokenCounterTraced.java Experiments.java && java Experiments
```

`TokenCounterTraced` is an instrumented, configurable copy used only for experiments; the graded program is `complete_code/TokenCounter.java`.
