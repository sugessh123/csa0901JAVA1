/** Station S4 is a daemon thread with a long batch; main ends first. */
public class DaemonDemo {
    public static void main(String[] args) throws Exception {
        boolean daemon = args.length > 0 && args[0].equals("daemon");
        Thread s4 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                try { Thread.sleep(200); } catch (InterruptedException e) { return; }
                System.out.println("  S4 scanned batch " + i + "/5");
            }
            System.out.println("  S4 finished all batches");
        }, "S4");
        s4.setDaemon(daemon);
        s4.start();
        Thread.sleep(500);
        System.out.println("main ends (S4 daemon=" + daemon + ", S4 alive=" + s4.isAlive() + ")");
    }
}
