import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Q1 - ATM withdrawal with explicit failure handling (try / multi-catch / finally). */
class InsufficientFundsException extends Exception {          // checked: recoverable business rule
    private final double shortBy;
    InsufficientFundsException(double shortBy) {
        super("Insufficient balance, short by Rs " + shortBy);
        this.shortBy = shortBy;
    }
    double getShortBy() { return shortBy; }
}

class InvalidAmountException extends IllegalArgumentException { // unchecked: caller supplied bad data
    InvalidAmountException(String msg) { super(msg); }
}

public class AtmWithdrawal {
    static final Map<Long, Double> BALANCES = new HashMap<>();
    static int sessionsClosed = 0;                              // counts finally executions

    static void readCard(Path cardFile) throws IOException {   // FileNotFoundException is a subclass
        try (BufferedReader r = Files.newBufferedReader(cardFile)) {
            r.readLine();
        }
    }

    static double withdraw(String accText, String amtText, Path cardFile) throws IOException, InsufficientFundsException {
        readCard(cardFile);                                     // IOException family (checked)
        long acc = Long.parseLong(accText.trim());              // NumberFormatException (unchecked)
        double amt = Double.parseDouble(amtText.trim());        // NumberFormatException (unchecked)
        if (amt <= 0) throw new InvalidAmountException("Amount must be positive: " + amt);
        Double bal = BALANCES.get(acc);
        if (bal == null) throw new InvalidAmountException("Unknown account " + acc);
        if (amt > bal) throw new InsufficientFundsException(amt - bal);
        BALANCES.put(acc, bal - amt);
        return bal - amt;
    }

    /** Wrapper that shows which catch block ran and whether finally ran. */
    static String attempt(String accText, String amtText, Path card) {
        String path;
        try {
            double left = withdraw(accText, amtText, card);
            path = "SUCCESS balance left=" + left;
        } catch (NoSuchFileException e) {                       // narrow IO exception first
            path = "catch NoSuchFileException -> card reader file missing: " + e.getMessage();
        } catch (IOException e) {                               // broader IO exception after it
            path = "catch IOException -> " + e.getMessage();
        } catch (InsufficientFundsException e) {                // checked business exception
            path = "catch InsufficientFundsException -> " + e.getMessage();
        } catch (NumberFormatException | InvalidAmountException e) { // multi-catch: sibling unchecked types
            path = "catch NumberFormatException|InvalidAmountException -> " + e.getMessage();
        } finally {
            sessionsClosed++;                                   // card is "ejected" on EVERY path
            System.out.print("  [finally: card ejected #" + sessionsClosed + "] ");
        }
        return path;
    }

    public static void main(String[] args) throws Exception {
        BALANCES.put(1001L, 5000.0);
        Path card = Files.createTempFile("card", ".dat");
        Files.writeString(card, "CARD-OK\n");
        Path missing = Path.of("/nonexistent/cardreader.dat");

        String[][] tests = {
            {"1001", "2000", "T1 valid withdrawal"},
            {"10O1", "500",  "T2 letters in account number"},
            {"1001", "-300", "T3 negative amount"},
            {"1001", "9999", "T4 amount above balance"},
            {"1001", "500",  "T5 card-reader file missing"},
            {"1001", "abc",  "T6 letters in amount"},
            {"7777", "100",  "T7 unknown account"},
        };
        System.out.println("=== Q1 ATM test run ===");
        for (String[] t : tests) {
            Path c = t[2].startsWith("T5") ? missing : card;
            String res = attempt(t[0], t[1], c);
            System.out.println(t[2] + " | in=(" + t[0] + "," + t[1] + ") -> " + res);
        }
        System.out.println("finally blocks executed: " + sessionsClosed + " of " + tests.length + " attempts");
        Files.deleteIfExists(card);
    }
}
