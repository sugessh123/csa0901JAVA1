import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Task B2 - deliberately damaged bus-pass file and how the file store responds. */
public class DamagedFileDemo {
    public static void main(String[] args) throws IOException {
        Path f = Files.createTempFile("damaged", ".csv");
        Files.writeString(f, String.join("\n",
            "P2001,192311001,Arun,12,4500.0",          // good
            "P2002,192311002,Divya,7",                 // wrong number of fields (4)
            "P2003,192311003,Kavin,9,FORTY-FIVE",      // non-numeric amount
            "",                                        // empty line
            "P2005,192311005,Meena,xx,3800.0",         // non-numeric route
            "P2006,192311006,Ravi,5,3000.0,EXTRA",     // too many fields
            "P2007,192311007,Sana,3,3500.0") + "\n");  // good

        PassFileStore s = new PassFileStore(f);
        List<String[]> good = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        s.readAll(good, errors);

        System.out.println("Valid records : " + good.size());
        for (String[] g : good) System.out.println("   " + Arrays.toString(g));
        System.out.println("Rejected lines: " + errors.size());
        for (String e : errors) System.out.println("   " + e);
        System.out.println("find(P2007) after the damaged lines -> " + Arrays.toString(s.find("P2007")));
        Files.delete(f);
    }
}
