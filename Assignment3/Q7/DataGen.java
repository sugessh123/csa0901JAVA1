import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Generates the 10,000-file score set with deterministic fault injection (rule on i % 100). */
public class DataGen {
    static List<Path> generate(Path dir, int n) throws IOException {
        Files.createDirectories(dir);
        List<Path> manifest = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            Path p = dir.resolve(String.format("score%05d.txt", i));
            manifest.add(p);
            int r = i % 100;
            if (r == 7 || r == 57) continue;                                   // MISSING: listed in manifest, not on disk
            String content;
            if (r == 13 || r == 63 || r == 93) {                              // CORRUPT, three flavours
                switch ((i / 100) % 3) {
                    case 0 -> content = String.format("S%05d,80,abc,70,60,50", i);
                    case 1 -> content = String.format("S%05d,80,70,60,50", i);
                    default -> content = String.format("S%05d,80,70,105,60,50", i);
                }
            } else if (r == 41) content = "";                                  // EMPTY
            else if (r == 88) content = valid(i - 1);                          // DUPLICATE: re-upload of file i-1
            else content = valid(i);
            Files.writeString(p, content);
        }
        return manifest;
    }
    static String valid(int i) {
        return String.format("S%05d,%d,%d,%d,%d,%d", i, (i * 7 + 13) % 101, (i * 7 + 26) % 101, (i * 7 + 39) % 101, (i * 7 + 52) % 101, (i * 7 + 65) % 101);
    }
    /** Expected counts derived from the injection rule, independent of the processor. */
    static EnumMap<Status, Integer> expected(int n) {
        EnumMap<Status, Integer> e = new EnumMap<>(Status.class);
        for (Status s : Status.values()) e.put(s, 0);
        for (int i = 1; i <= n; i++) {
            int r = i % 100;
            Status s = (r == 7 || r == 57) ? Status.MISSING : (r == 13 || r == 63 || r == 93) ? Status.CORRUPT
                     : r == 41 ? Status.EMPTY : r == 88 ? Status.DUPLICATE : Status.OK;
            e.merge(s, 1, Integer::sum);
        }
        return e;
    }
}
