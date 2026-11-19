import java.sql.*;
import java.util.*;

/** Task B3: correctness (B3.1), size comparison (B3.2), auto-commit vs transaction (B3.3).
 *  Usage: java -cp .:mysql-connector-j.jar LoaderBenchmark [maxRows]   (default 100000) */
public class LoaderBenchmark {
    static final String SQL = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";

    static List<String[]> generate(int n) {
        List<String[]> l = new ArrayList<>(n);
        for (int i = 1; i <= n; i++)
            l.add(new String[]{"P" + i, "19231" + (100000 + i), "Student" + i, String.valueOf(i % 20 + 1), "4500.0"});
        return l;
    }
    static void clear(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) { st.executeUpdate("DELETE FROM bus_pass"); }
    }
    static int count(Connection con) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            rs.next(); return rs.getInt(1);
        }
    }

    // (a) plain Statement with concatenated SQL, auto-commit ON
    static void viaStatement(Connection con, List<String[]> rows) throws SQLException {
        con.setAutoCommit(true);
        try (Statement st = con.createStatement()) {
            for (String[] p : rows)
                st.executeUpdate("INSERT INTO bus_pass VALUES ('" + p[0] + "','" + p[1] + "','" + p[2] + "',"
                        + p[3] + "," + p[4] + ")");
        }
    }
    // (b) PreparedStatement, one executeUpdate per row, auto-commit ON
    static void viaPrepared(Connection con, List<String[]> rows) throws SQLException {
        con.setAutoCommit(true);
        try (PreparedStatement ps = con.prepareStatement(SQL)) {
            for (String[] p : rows) {
                ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
                ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4]));
                ps.executeUpdate();
            }
        }
    }
    // (c) PreparedStatement + addBatch/executeBatch (auto-commit ON, batches of 1000)
    static void viaBatchAutoCommit(Connection con, List<String[]> rows) throws SQLException {
        con.setAutoCommit(true);
        try (PreparedStatement ps = con.prepareStatement(SQL)) {
            int n = 0;
            for (String[] p : rows) {
                ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
                ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4]));
                ps.addBatch();
                if (++n % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
    }

    static void b31(Connection con) throws SQLException {
        System.out.println("=== B3.1 Correctness on 10 records ===");
        List<String[]> ten = generate(10);
        String[] names = {"Statement", "PreparedStatement", "Batch"};
        for (int k = 0; k < 3; k++) {
            clear(con);
            if (k == 0) viaStatement(con, ten); else if (k == 1) viaPrepared(con, ten); else viaBatchAutoCommit(con, ten);
            System.out.println(names[k] + " -> rows in table: " + count(con));
        }
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM bus_pass ORDER BY route_no, pass_id")) {
            ResultSetMetaData md = rs.getMetaData();
            System.out.println("Columns (name : SQL type : Java class):");
            for (int i = 1; i <= md.getColumnCount(); i++)
                System.out.println("  " + md.getColumnName(i) + " : " + md.getColumnTypeName(i) + " : " + md.getColumnClassName(i));
            int shown = 0;
            while (rs.next() && shown++ < 3)
                System.out.println("  " + rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getInt(4) + " | " + rs.getDouble(5));
        }
    }

    static void b32(Connection con, int maxRows) throws SQLException {
        System.out.println("\n=== B3.2 Time (ms) as data grows ===");
        System.out.printf("%-10s %-14s %-18s %-18s%n", "rows", "Statement", "PreparedStatement", "Prepared+Batch");
        for (int n : new int[]{1000, 10000, 100000}) {
            if (n > maxRows) break;
            List<String[]> rows = generate(n);
            long[] t = new long[3];
            for (int k = 0; k < 3; k++) {
                clear(con);
                long t0 = System.nanoTime();
                if (k == 0) viaStatement(con, rows); else if (k == 1) viaPrepared(con, rows); else viaBatchAutoCommit(con, rows);
                t[k] = (System.nanoTime() - t0) / 1_000_000;
            }
            System.out.printf("%-10d %-14d %-18d %-18d%n", n, t[0], t[1], t[2]);
        }
    }

    static void b33(Connection con) throws SQLException {
        System.out.println("\n=== B3.3 Auto-commit vs one transaction (10,000 rows) ===");
        List<String[]> rows = generate(10000);

        clear(con);
        long t0 = System.nanoTime(); viaBatchAutoCommit(con, rows);
        long autoMs = (System.nanoTime() - t0) / 1_000_000;
        clear(con);
        t0 = System.nanoTime(); PassLoader.loadAll(con, rows);
        long txMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("Batch with auto-commit ON : " + autoMs + " ms");
        System.out.println("Batch in ONE transaction  : " + txMs + " ms");

        List<String[]> bad = generate(10000);
        bad.get(5000)[4] = "NOT-A-NUMBER";                      // invalid amount half-way
        clear(con);
        try { viaBatchAutoCommit(con, bad); } catch (RuntimeException e) { System.out.println("Auto-commit failure : " + e); }
        System.out.println("Rows left after failure (auto-commit)  : " + count(con) + "   <- partial data stays");
        clear(con);
        try { PassLoader.loadAll(con, bad); } catch (RuntimeException e) { System.out.println("Transaction failure : " + e); }
        System.out.println("Rows left after failure (transaction)  : " + count(con) + "   <- rolled back, table clean");
    }

    public static void main(String[] args) throws SQLException {
        int maxRows = args.length > 0 ? Integer.parseInt(args[0]) : 100000;
        try (Connection con = Db.open()) {
            b31(con); b32(con, maxRows); b33(con);
            clear(con);
        }
    }
}
