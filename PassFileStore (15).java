import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Full bus-pass file store designed in Task A1 (text format).
 * Line format: passId,regNo,name,route,amount   (name is escaped: ',' -> %2C, '%' -> %25)
 */
public class PassFileStore {
    private final Path file;

    public PassFileStore(Path file) { this.file = file; }

    static String escape(String s)   { return s.replace("%", "%25").replace(",", "%2C"); }
    static String unescape(String s) { return s.replace("%2C", ",").replace("%25", "%"); }

    public void add(String passId, String regNo, String name, int route, double amount)
            throws IOException {
        String line = String.join(",", passId, regNo, escape(name),
                String.valueOf(route), String.valueOf(amount));
        try (BufferedWriter w = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        }
    }

    /** Returns the fields (name already unescaped) or null with a reason in why[0]. */
    public static String[] parse(String line, String[] why) {
        if (line.isBlank())      { why[0] = "empty line"; return null; }
        String[] f = line.split(",", -1);
        if (f.length != 5)       { why[0] = "expected 5 fields but found " + f.length; return null; }
        if (f[0].isEmpty())      { why[0] = "missing pass ID"; return null; }
        try { Integer.parseInt(f[3]); }
        catch (NumberFormatException e) { why[0] = "route is not an integer: " + f[3]; return null; }
        try {
            double a = Double.parseDouble(f[4]);
            if (a < 0 || Double.isNaN(a)) { why[0] = "invalid amount: " + f[4]; return null; }
        } catch (NumberFormatException e) { why[0] = "amount is not numeric: " + f[4]; return null; }
        f[2] = unescape(f[2]);
        return f;
    }

    public String[] find(String passId) throws IOException {
        String[] why = new String[1];
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = parse(line, why);
                if (f != null && f[0].equals(passId)) return f;
            }
        }
        return null;
    }

    public List<String[]> listByRoute(int route) throws IOException {
        List<String[]> out = new ArrayList<>();
        String[] why = new String[1];
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = parse(line, why);
                if (f != null && Integer.parseInt(f[3]) == route) out.add(f);
            }
        }
        return out;
    }

    /** Reads every line; valid rows go to good, a message per bad line goes to errors. */
    public void readAll(List<String[]> good, List<String> errors) throws IOException {
        String[] why = new String[1];
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            int n = 0;
            while ((line = r.readLine()) != null) {
                n++;
                String[] f = parse(line, why);
                if (f == null) errors.add("line " + n + ": " + why[0] + "  -> [" + line + "]");
                else good.add(f);
            }
        }
    }
}
