/** One run prints one line: the finish order. Run it 10+ times from run10.sh. Optional arg: priority */
public class ScanOrder {
    public static void main(String[] args) throws Exception {
        boolean prio = args.length > 0 && args[0].equals("priority");
        Thread[] ts = new Thread[4];
        for (int i = 0; i < 4; i++) ts[i] = new StationThread("S" + (i + 1), ScanStations.BATCH[i]);
        if (prio) { ts[0].setPriority(Thread.MIN_PRIORITY); ts[1].setPriority(Thread.MAX_PRIORITY);
                    ts[2].setPriority(Thread.NORM_PRIORITY); ts[3].setPriority(Thread.NORM_PRIORITY); }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        System.out.println(Counter.FINISH_ORDER);
    }
}
