package samuraychik.chessbot.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static volatile DatabaseConnection instance;
    private final Connection connection;

    private DatabaseConnection(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
    }

    @SuppressWarnings("DoubleCheckedLocking")
    public static DatabaseConnection getInstance(String url, String user, String password) throws SQLException {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection(url, user, password);
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
