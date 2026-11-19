import java.util.*;

/** Instrumented, configurable copy of the counter for experiments (the graded program is complete_code/TokenCounter.java). */
class TokenCounterTraced {
    final int[] buffer; int count, in, out, maxSeen = Integer.MIN_VALUE, minSeen = Integer.MAX_VALUE;
    final boolean useWhile, useNotifyAll; final List<Integer> sizeLog; final boolean trace;
    TokenCounterTraced(int cap, boolean useWhile, boolean useNotifyAll, boolean trace) {
        buffer = new int[cap]; this.useWhile = useWhile; this.useNotifyAll = useNotifyAll;
        this.trace = trace; sizeLog = trace ? new ArrayList<>() : null; maxSeen = 0; minSeen = 0;
    }
    private void record() { maxSeen = Math.max(maxSeen, count); minSeen = Math.min(minSeen, count); if (trace && sizeLog.size() < 4000) sizeLog.add(count); }
    synchronized void put(int t) throws InterruptedException {
        if (useWhile) { while (count == buffer.length) wait(); } else if (count == buffer.length) wait();
        buffer[in] = t; in = (in + 1) % buffer.length; count++; record();
        if (useNotifyAll) notifyAll(); else notify();
    }
    synchronized int take() throws InterruptedException {
        if (useWhile) { while (count == 0) wait(); } else if (count == 0) wait();
        int t = buffer[out]; out = (out + 1) % buffer.length; count--; record();
        if (useNotifyAll) notifyAll(); else notify();
        return t;
    }
}
