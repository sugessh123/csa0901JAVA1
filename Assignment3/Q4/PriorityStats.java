import java.util.*;
/** Counts which station finishes first over many trials, with and without priorities. */
public class PriorityStats {
    static int[] trial(boolean prio, int trials) throws Exception {
        int[] first = new int[4];
        for (int k = 0; k < trials; k++) {
            Counter.reset();
            Thread[] ts = new Thread[4];
            for (int i = 0; i < 4; i++) ts[i] = new StationThread("S" + (i + 1), 40);
            if (prio) { ts[0].setPriority(Thread.MIN_PRIORITY); ts[1].setPriority(Thread.MAX_PRIORITY); }
            for (Thread t : ts) t.start();
            for (Thread t : ts) t.join();
            first[Integer.parseInt(Counter.FINISH_ORDER.get(0).substring(1)) - 1]++;
        }
        return first;
    }
    public static void main(String[] a) throws Exception {
        int n = 100;
        System.out.println("Station finishing FIRST, out of " + n + " trials (S1=MIN, S2=MAX when priorities are set)");
        System.out.println("default priorities : " + Arrays.toString(trial(false, n)) + "   (index 0 = S1 ... index 3 = S4)");
        System.out.println("S1=MIN, S2=MAX     : " + Arrays.toString(trial(true, n)));
    }
}
