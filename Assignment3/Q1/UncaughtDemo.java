/** Q1 Student B - an exception nobody catches, to read its stack trace. */
public class UncaughtDemo {
    static long parseAccount(String s) { return Long.parseLong(s); }
    static void withdraw(String acc) { parseAccount(acc); }
    public static void main(String[] args) {
        System.out.println("Starting withdrawal...");
        withdraw("10O1");                      // letter O instead of zero
        System.out.println("never reached");
    }
}
