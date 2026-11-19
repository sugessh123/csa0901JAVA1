import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Task B4 - migrate the bus-pass text file into MySQL (batches + one transaction),
 *  write a failure report, then inspect the database with DatabaseMetaData.
 *  Usage: java -cp .:mysql-connector-j.jar Migration [passes.csv] */
public class Migration {
    public static void main(String[] args) throws IOException, SQLException {
        Path src = args.length > 0 ? Paths.get(args[0]) : sampleFile();
        PassFileStore store = new PassFileStore(src);
        List<String[]> good = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        store.readAll(good, errors);                                   // validation happens here
        System.out.println("Read " + (good.size() + errors.size()) + " lines: "
                + good.size() + " valid, " + errors.size() + " failed validation");

        try (Connection con = Db.open()) {
            try (Statement st = con.createStatement()) { st.executeUpdate("DELETE FROM bus_pass"); }
            long t0 = System.nanoTime();
            int loaded = PassLoader.loadAll(con, good);                // batches + one transaction
            System.out.println("Loaded " + loaded + " rows in " + (System.nanoTime() - t0) / 1_000_000 + " ms");

            Path report = Paths.get("failure_report.txt");
            Files.write(report, errors);
            System.out.println("Failure report written to " + report + " (" + errors.size() + " entries)");
            for (String e : errors) System.out.println("   " + e);

            // B4.2 DatabaseMetaData
            DatabaseMetaData md = con.getMetaData();
            System.out.println("\n--- DatabaseMetaData ---");
            System.out.println("Database product : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("Driver           : " + md.getDriverName() + " " + md.getDriverVersion());
            System.out.println("JDBC version     : " + md.getJDBCMajorVersion() + "." + md.getJDBCMinorVersion());
            System.out.println("Supports batch   : " + md.supportsBatchUpdates());
            System.out.println("Supports txns    : " + md.supportsTransactions());
            System.out.println("Tables in busdb_test:");
            try (ResultSet rs = md.getTables("busdb_test", null, "%", new String[]{"TABLE"})) {
                while (rs.next()) System.out.println("   " + rs.getString("TABLE_NAME"));
            }
        }
    }

    /** Small sample file with two deliberately bad lines so the failure report is not empty. */
    static Path sampleFile() throws IOException {
        Path p = Files.createTempFile("passes", ".csv");
        PassFileStore s = new PassFileStore(p);
        for (int i = 1; i <= 2500; i++) s.add("P" + i, "19231" + (100000 + i), "Student" + i, i % 20 + 1, 4500);
        Files.writeString(p, "P9001,192319001,Broken\nP9002,192319002,BadAmt,4,abc\n", StandardOpenOption.APPEND);
        return p;
    }
}
