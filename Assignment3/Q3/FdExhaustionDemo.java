import java.nio.file.*;
import java.util.*;
/** Q3 Student A - reproduces the real "Too many open files" error with a version that never closes. */
public class FdExhaustionDemo {
    public static void main(String[] args) throws Exception {
        Path d = Files.createTempDirectory("fd");
        List<Path> st = SalesReport.makeStores(d, 400, 20, 0);
        String mode = args.length > 0 ? args[0] : "noclose";
        ReportGenerator g = mode.equals("twr") ? new SalesReport.TryWithResources() : new SalesReport.NoClose();
        try {
            Report r = g.run(st);
            System.out.println(g.name() + ": processed " + r.perStore.size() + " stores, failed " + r.failed.size());
        } catch (Exception e) {
            System.out.println(g.name() + ": " + e);
        }
    }
}
