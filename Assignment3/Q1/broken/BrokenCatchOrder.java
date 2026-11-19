import java.io.*;
/** Q1 Student B - broader catch BEFORE narrower catch: does not compile. */
public class BrokenCatchOrder {
    static void read() throws IOException { throw new FileNotFoundException("card.dat"); }
    public static void main(String[] args) {
        try {
            read();
        } catch (IOException e) {              // broad first
            System.out.println("IO");
        } catch (FileNotFoundException e) {    // unreachable
            System.out.println("FNF");
        }
    }
}
