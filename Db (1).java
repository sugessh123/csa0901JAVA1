import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Connection helper for the LOCAL TEST database created only for this assignment.
 *  Set DB_URL, DB_USER, DB_PASSWORD environment variables to override the defaults. */
public class Db {
    static final String URL  = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://localhost:3306/busdb_test?useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true");
    static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    static final String PASS = System.getenv().getOrDefault("DB_PASSWORD", "");

    static Connection open() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
