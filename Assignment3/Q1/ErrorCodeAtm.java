import java.util.*;
/** Q1 alternative - no exceptions: validate first, return error codes. Also benchmarks throw vs return code. */
public class ErrorCodeAtm {
    static final int OK = 0, BAD_ACC = -1, BAD_AMT = -2, NO_FUNDS = -3;
    static long sink;

    static int withdrawCode(String acc, String amt, double balance) {
        Scanner sa = new Scanner(acc), sm = new Scanner(amt);
        if (!sa.hasNextLong()) return BAD_ACC;          // check 1
        if (!sm.hasNextDouble()) return BAD_AMT;        // check 2
        sa.nextLong(); double a = sm.nextDouble();
        if (a <= 0) return BAD_AMT;                     // check 3
        if (a > balance) return NO_FUNDS;               // check 4
        return OK;
    }
    static int parseCode(String s) {                    // cheap check without Scanner
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return -1;
        return s.isEmpty() ? -1 : 0;
    }
    static class NoTrace extends RuntimeException {
        NoTrace() { super("bad", null, false, false); } // writableStackTrace = false
    }
    static int depth(int d, int mode) {
        if (d > 0) return depth(d - 1, mode) + 1;
        if (mode == 0) { try { Long.parseLong("10O1"); } catch (NumberFormatException e) { return 1; } return 0; }
        if (mode == 1) return parseCode("10O1");
        try { throw new NoTrace(); } catch (NoTrace e) { return 1; }
    }
    static long bench(int d, int mode, int n) {
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) sink += depth(d, mode);
        return (System.nanoTime() - t0) / n;
    }
    public static void main(String[] args) {
        System.out.println("=== error-code ATM sample ===");
        String[][] in = {{"1001","2000"},{"10O1","500"},{"1001","-5"},{"1001","9999"},{"1001","abc"}};
        for (String[] s : in) System.out.println(Arrays.toString(s) + " -> code " + withdrawCode(s[0], s[1], 5000));
        int n = 200_000;
        for (int w = 0; w < 3; w++) { bench(10,0,n); bench(10,1,n); bench(10,2,n); }  // warm-up
        System.out.println("\n=== ns per failure (avg of " + n + " iterations) ===");
        System.out.printf("%-10s %14s %14s %22s%n", "stackDepth", "throw+catch", "return code", "throw, no stack trace");
        for (int d : new int[]{10, 100, 500}) {
            System.out.printf("%-10d %14d %14d %22d%n", d, bench(d,0,n), bench(d,1,n), bench(d,2,n));
        }
        System.out.println("(sink=" + sink + ")");
    }
}
