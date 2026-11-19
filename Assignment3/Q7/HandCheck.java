import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Q7 Student B - hand-checked 12-file set, edge cases, and the timeout test. */
public class HandCheck {
    static int pass = 0, fail = 0;
    static void check(String what, Object expected, Object actual) {
        boolean ok = expected.equals(actual);
        if (ok) pass++; else fail++;
        System.out.printf("  %-52s expected=%-8s actual=%-8s %s%n", what, expected, actual, ok ? "PASS" : "FAIL");
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("hand12");
        String[] content = {
            "S00001,80,70,90,60,75",   // f01 total 375  75.0 -> A
            "S00002,50,60,70,80,90",   // f02 total 350  70.0 -> B
            "S00003,100,100,100,100,100", // f03 total 500 100.0 -> O
            "S00004,0,0,0,0,0",        // f04 total 0     0.0 -> F
            "S00005,45,55,65,75,85",   // f05 total 325  65.0 -> B
            "S00006,33,66,99,100,2",   // f06 total 300  60.0 -> B
            null,                      // f07 MISSING (never created)
            "S00008,80,x7,60,50,40",   // f08 CORRUPT: letter in a mark
            "S00009,80,70,60,50",      // f09 CORRUPT: only 5 fields
            "S00010,80,70,105,60,50",  // f10 CORRUPT: mark above 100
            "",                        // f11 EMPTY
            "S00001,80,70,90,60,75"    // f12 DUPLICATE: same student re-uploaded
        };
        List<Path> manifest = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Path p = dir.resolve(String.format("f%02d.txt", i + 1));
            manifest.add(p);
            if (content[i] != null) Files.writeString(p, content[i]);
        }
        System.out.println("=== H1. Hand-checked 12-file set (pool of 4 threads) ===");
        System.out.println("Hand calculation: OK=6 (f01-f06), MISSING=1 (f07), CORRUPT=3 (f08,f09,f10), EMPTY=1 (f11), DUPLICATE=1 (f12)");
        System.out.println("                  sum of totals of the 6 OK students = 375+350+500+0+325+300 = 1850; grades A=1 B=3 O=1 F=1");
        Summary s = ResultProcessor.process(manifest, 4, 0, 2000);
        System.out.println("Program : " + s.render());
        check("OK count", 6, s.count(Status.OK));
        check("MISSING count", 1, s.count(Status.MISSING));
        check("CORRUPT count", 3, s.count(Status.CORRUPT));
        check("EMPTY count", 1, s.count(Status.EMPTY));
        check("DUPLICATE count", 1, s.count(Status.DUPLICATE));
        int sumTotals = 0; Map<Character, Integer> grades = new TreeMap<>();
        for (ScoreResult r : s.results.values()) { sumTotals += r.total(); grades.merge(r.grade(), 1, Integer::sum); }
        check("sum of totals", 1850, sumTotals);
        check("grade distribution", "{A=1, B=3, F=1, O=1}", grades.toString());
        check("summary complete (every file accounted)", true, s.complete());
        System.out.println("  failure lines:"); s.failures.stream().sorted().forEach(x -> System.out.println("    " + x));

        System.out.println("\n=== H2. Edge cases: what the processor does with awkward files ===");
        Path ed = Files.createTempDirectory("edge");
        Map<String, Status> want = new LinkedHashMap<>();
        List<Path> em = new ArrayList<>();
        class E { void add(String label, Path p, Status st) { em.add(p); want.put(p.getFileName().toString() + "|" + label, st); } }
        E e = new E();
        Path p;
        p = ed.resolve("e01.txt"); Files.write(p, "\uFEFFS10001,1,2,3,4,5".getBytes(StandardCharsets.UTF_8)); e.add("UTF-8 BOM before id", p, Status.OK);
        p = ed.resolve("e02.txt"); Files.writeString(p, "S10002,80.5,70,60,50,40"); e.add("decimal mark 80.5", p, Status.CORRUPT);
        p = ed.resolve("e03.txt"); Files.createDirectory(p); e.add("a directory, not a file", p, Status.IO_ERROR);
        p = ed.resolve("e04.txt"); try { Files.createSymbolicLink(p, ed.resolve("nowhere")); } catch (Exception ex) { Files.writeString(p, ""); } e.add("dangling symlink", p, Files.isSymbolicLink(p) ? Status.MISSING : Status.EMPTY);
        p = ed.resolve("e05.txt"); Files.writeString(p, "S10005,1,2,3,4,5\nS10006,1,2,3,4,5"); e.add("two records in one file", p, Status.CORRUPT);
        p = ed.resolve("e06.txt"); Files.writeString(p, "S10007,1,2,3,4,5\r\n\r\n"); e.add("CRLF + trailing blank lines", p, Status.OK);
        p = ed.resolve("e07.txt"); Files.writeString(p, "S10008,0,0,0,0,100"); e.add("boundary marks 0 and 100", p, Status.OK);
        p = ed.resolve("e08.txt"); Files.writeString(p, "S10009,-1,2,3,4,5"); e.add("negative mark", p, Status.CORRUPT);
        p = ed.resolve("e09.txt"); Files.writeString(p, "S10010," + "9".repeat(5_000_000)); e.add("5 MB single line (memory guard)", p, Status.CORRUPT);
        p = ed.resolve("e10.txt"); Files.write(p, "S10011,1,2,3,4,5".getBytes(StandardCharsets.UTF_16)); e.add("UTF-16 encoded file", p, Status.OK);
        p = ed.resolve("e11.txt"); Files.writeString(p, " S10012 , 1 , 2 , 3 , 4 , 5 "); e.add("spaces around every field", p, Status.OK);
        p = ed.resolve("e12.txt"); Files.writeString(p, "S10013,1,2,3,4,99999999999"); e.add("mark overflowing int", p, Status.CORRUPT);
        Summary es = ResultProcessor.process(em, 4, 0, 5000);
        Map<String, String> actual = new HashMap<>();
        // run each one alone to learn its individual status
        for (Path q : em) {
            Summary one = ResultProcessor.process(List.of(q), 1, 0, 2000);
            for (Status st : Status.values()) if (one.count(st) > 0) actual.put(q.getFileName().toString(), st.toString());
        }
        for (Map.Entry<String, Status> en : want.entrySet()) {
            String file = en.getKey().substring(0, en.getKey().indexOf('|'));
            check(en.getKey().substring(en.getKey().indexOf('|') + 1), en.getValue().toString(), actual.get(file));
        }
        System.out.println("  edge-set summary: " + es.render());

        System.out.println("\n=== H3. Timeout: one worker never returns in time ===");
        Path td = Files.createTempDirectory("hang");
        Files.writeString(td.resolve("a.txt"), "S20001,1,2,3,4,5");
        Files.writeString(td.resolve("b.txt"), "S20002,1,2,3,4,5");
        Files.writeString(td.resolve("c.txt"), "HANG");
        Summary ts = ResultProcessor.process(List.of(td.resolve("a.txt"), td.resolve("b.txt"), td.resolve("c.txt")), 2, 0, 500);
        System.out.println("Program : " + ts.render());
        check("OK count", 2, ts.count(Status.OK));
        check("TIMEOUT count", 1, ts.count(Status.TIMEOUT));
        check("elapsed well below the 5000 ms hang (ms < 2000)", true, ts.elapsedMs < 2000);
        System.out.println("\nRESULT: " + pass + " passed, " + fail + " failed");
    }
}
