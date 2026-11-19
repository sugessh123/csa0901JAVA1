import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Task B1: validation tests (B1.1), buffering measurement (B1.3), growth measurement (B1.4),
 *  and the HashMap alternative. Usage: java B1Experiments [maxMB]   (default 50) */
public class B1Experiments {

    // ---------- B1.1 tests ----------
    static void tests() throws IOException {
        System.out.println("=== B1.1 Validation tests ===");
        Path f = Files.createTempFile("buspass", ".csv");
        PassFileStore s = new PassFileStore(f);
        s.add("P1001", "192311001", "Arun", 12, 4500);
        s.add("P1002", "192311002", "Meena, K", 7, 3800);          // name contains the delimiter
        Files.writeString(f, "P1003,DAMAGED\n", StandardOpenOption.APPEND); // damaged line
        s.add("P1004", "192311004", "Divya", 12, 4200);
        System.out.println("T1 add+find normal  (P1001)  -> " + Arrays.toString(s.find("P1001")));
        System.out.println("T2 name with comma  (P1002)  -> " + Arrays.toString(s.find("P1002")));
        System.out.println("T3 missing ID       (P9999)  -> " + Arrays.toString(s.find("P9999")));
        System.out.println("T4 damaged line ID  (P1003)  -> " + Arrays.toString(s.find("P1003")));
        System.out.println("T5 find after damage (P1004) -> " + Arrays.toString(s.find("P1004")));
        System.out.print  ("T6 list route 12             -> ");
        for (String[] r : s.listByRoute(12)) System.out.print(r[0] + " ");
        System.out.println();
        Files.delete(f);
    }

    // ---------- B1.3 line counters ----------
    static long countUnbuffered(Path p) throws IOException {
        long lines = 0;
        try (FileInputStream in = new FileInputStream(p.toFile())) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
        }
        return lines;
    }
    static long countBuffered(Path p) throws IOException {
        long lines = 0;
        try (InputStream in = new FileInputStream(p.toFile())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) for (int i = 0; i < n; i++) if (buf[i] == '\n') lines++;
        }
        return lines;
    }
    static long countBufferedReader(Path p) throws IOException {
        long lines = 0;
        try (BufferedReader r = Files.newBufferedReader(p)) { while (r.readLine() != null) lines++; }
        return lines;
    }
    static long countReadAllLines(Path p) throws IOException { return Files.readAllLines(p).size(); }

    static Path makeFile(int mb) throws IOException {
        Path p = Files.createTempFile("attendance" + mb + "mb", ".log");
        long target = mb * 1024L * 1024L;
        try (BufferedWriter w = Files.newBufferedWriter(p)) {
            long size = 0; int i = 0;
            while (size < target) {
                String line = "192311" + String.format("%03d", i % 1000) + ",2026-11-" + (1 + i % 28) + ",PRESENT";
                w.write(line); w.newLine();
                size += line.length() + 1; i++;
            }
        }
        return p;
    }

    static void buffering(int maxMB) throws IOException {
        System.out.println("\n=== B1.3 Effect of buffering (line counting) ===");
        System.out.printf("%-8s %-8s %-14s %-14s %-14s %-14s%n",
                "MB", "lines", "unbuffered ms", "byte[8192] ms", "BufferedReader", "readAllLines");
        for (int mb : new int[]{1, 10, 50}) {
            if (mb > maxMB) break;
            Path p = makeFile(mb);
            long t0 = System.nanoTime(); long a = countUnbuffered(p);
            long t1 = System.nanoTime(); long b = countBuffered(p);
            long t2 = System.nanoTime(); long c = countBufferedReader(p);
            long t3 = System.nanoTime(); long d = countReadAllLines(p);
            long t4 = System.nanoTime();
            if (a != b || b != c || c != d) System.out.println("MISMATCH " + a + " " + b + " " + c + " " + d);
            System.out.printf("%-8d %-8d %-14.1f %-14.1f %-14.1f %-14.1f%n", mb, a,
                    (t1 - t0) / 1e6, (t2 - t1) / 1e6, (t3 - t2) / 1e6, (t4 - t3) / 1e6);
            Files.delete(p);
        }
    }

    // ---------- B1.4 growth + HashMap alternative ----------
    static void growth() throws IOException {
        System.out.println("\n=== B1.4 Growth: time to find a pass (worst case = last record) ===");
        System.out.printf("%-10s %-16s %-20s %-18s%n",
                "records", "file find ms", "100 file finds ms", "HashMap build+100 ms");
        for (int n : new int[]{1000, 5000, 10000, 50000, 100000}) {
            Path f = Files.createTempFile("growth", ".csv");
            PassFileStore s = new PassFileStore(f);
            try (BufferedWriter w = Files.newBufferedWriter(f)) {      // fast bulk generation
                for (int i = 1; i <= n; i++)
                    w.write("P" + i + ",19231" + (100000 + i) + ",Student" + i + "," + (i % 20 + 1) + ",4500.0\n");
            }
            String last = "P" + n;
            s.find(last);                                              // warm-up
            long t0 = System.nanoTime(); String[] r = s.find(last);
            long t1 = System.nanoTime();
            for (int q = 0; q < 100; q++) s.find(last);
            long t2 = System.nanoTime();
            List<String[]> good = new ArrayList<>(); List<String> err = new ArrayList<>();
            s.readAll(good, err);
            Map<String, String[]> map = new HashMap<>();
            for (String[] g : good) map.put(g[0], g);
            for (int q = 0; q < 100; q++) map.get(last);
            long t3 = System.nanoTime();
            if (r == null) System.out.println("NOT FOUND?");
            System.out.printf("%-10d %-16.2f %-20.1f %-18.1f%n", n,
                    (t1 - t0) / 1e6, (t2 - t1) / 1e6, (t3 - t2) / 1e6);
            Files.delete(f);
        }
    }

    public static void main(String[] args) throws IOException {
        int maxMB = args.length > 0 ? Integer.parseInt(args[0]) : 50;
        System.out.println("Java " + System.getProperty("java.version") + ", OS " + System.getProperty("os.name"));
        tests();
        buffering(maxMB);
        growth();
    }
}
