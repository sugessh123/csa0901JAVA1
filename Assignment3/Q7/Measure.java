import java.nio.file.*;
import java.util.*;

/** Q7 Student B - total time for 10,000 files with pools of 1,2,4,8 (and 16,32,64) threads. Verifies counts every run. */
public class Measure {
    public static void main(String[] args) throws Exception {
        int n = 10_000;
        Path dir = Files.createTempDirectory("scores10k");
        List<Path> files = DataGen.generate(dir, n);
        EnumMap<Status, Integer> exp = DataGen.expected(n);
        System.out.println("Dataset: " + n + " files. Expected from the injection rule: " + exp);
        int[] pools = {1, 2, 4, 8, 16, 32, 64};
        long[] latencies = {0, 1};
        for (long lat : latencies) {
            int reps = lat == 0 ? 5 : 2;
            System.out.println("\n--- simulated storage latency per file = " + lat + " ms, " + reps + " repetitions ---");
            System.out.printf("%-6s %10s %10s %10s   %s%n", "pool", "mean ms", "min ms", "max ms", "counts correct & summary complete");
            for (int p : pools) {
                for (int w = 0; w < (lat == 0 ? 2 : 0); w++) ResultProcessor.process(files, p, lat, 30_000);   // warm-up
                double sum = 0, min = 1e18, max = 0; boolean allOk = true;
                for (int r = 0; r < reps; r++) {
                    Summary s = ResultProcessor.process(files, p, lat, 30_000);
                    sum += s.elapsedMs; min = Math.min(min, s.elapsedMs); max = Math.max(max, s.elapsedMs);
                    for (Status st : Status.values()) if (!exp.get(st).equals(s.count(st))) allOk = false;
                    if (!s.complete()) allOk = false;
                }
                System.out.printf("%-6d %10.0f %10.0f %10.0f   %s%n", p, sum / reps, min, max, allOk);
            }
        }
    }
}
