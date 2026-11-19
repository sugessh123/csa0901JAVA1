import java.sql.*;

/** Task B2 - unsafe (string concatenation) vs safe (PreparedStatement) login check. */
public class LoginDemo {

    // UNSAFE: the input becomes part of the SQL text
    static boolean unsafeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        System.out.println("   SQL sent  : " + sql);
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }

    // SAFE: the SQL text is fixed; input is sent only as data
    static boolean safeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id=? AND pwd=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // WEAKER alternative: validate with a regular expression before building the string
    static boolean regexLogin(Connection con, String id, String pwd) throws SQLException {
        if (!id.matches("[A-Za-z0-9_]{1,30}") || !pwd.matches("[A-Za-z0-9@#_]{1,60}")) {
            return false;                       // rejects quotes, but also rejects legal passwords like "it's me"
        }
        return unsafeLogin(con, id, pwd);
    }

    public static void main(String[] args) throws SQLException {
        String[][] cases = {
            {"admin", "S3cret#99",  "valid login"},
            {"admin", "wrong",      "wrong password"},
            {"admin' -- ", "x",     "comment attack: everything after -- is ignored"},
            {"x", "' OR '1'='1",    "tautology attack: OR '1'='1' is always true"},
        };
        try (Connection con = Db.open()) {
            for (String[] c : cases) {
                System.out.println("\nCase: " + c[2] + "   id=[" + c[0] + "] pwd=[" + c[1] + "]");
                System.out.println("   UNSAFE accepted? " + unsafeLogin(con, c[0], c[1]));
                System.out.println("   SAFE   accepted? " + safeLogin(con, c[0], c[1]));
                System.out.println("   REGEX  accepted? " + regexLogin(con, c[0], c[1]));
            }
        }
    }
}
