import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
/** Q5 alternative: heap bytes allocated to represent s seats. */
public class SpaceCost {
    static final com.sun.management.ThreadMXBean T = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static long alloc() { return T.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    static Object keep;
    public static void main(String[] a) {
        int s = 10_000;
        for (int w = 0; w < 3; w++) { keep = new boolean[s]; }
        long b0 = alloc(); keep = new boolean[s]; long boolBytes = alloc() - b0;
        b0 = alloc(); AtomicBoolean[] arr = new AtomicBoolean[s]; for (int i = 0; i < s; i++) arr[i] = new AtomicBoolean(); keep = arr; long atomBytes = alloc() - b0;
        b0 = alloc(); ConcurrentHashMap<Integer, Boolean> m = new ConcurrentHashMap<>(); for (int i = 0; i < s; i++) m.putIfAbsent(i, Boolean.TRUE); keep = m; long mapBytes = alloc() - b0;
        System.out.println("Heap used to represent s = " + s + " seats (all booked in the map case):");
        System.out.printf("  boolean[s]            : %8d bytes (%.1f bytes/seat)%n", boolBytes, boolBytes / (double) s);
        System.out.printf("  AtomicBoolean[s]      : %8d bytes (%.1f bytes/seat)%n", atomBytes, atomBytes / (double) s);
        System.out.printf("  ConcurrentHashMap     : %8d bytes (%.1f bytes/seat)%n", mapBytes, mapBytes / (double) s);
    }
}
