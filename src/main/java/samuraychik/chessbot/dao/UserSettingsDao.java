package samuraychik.chessbot.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import samuraychik.chessbot.model.Level;
import samuraychik.chessbot.model.UserSettings;

public class UserSettingsDao {

    private final Connection connection;

    public UserSettingsDao(Connection connection) {
        this.connection = connection;
    }

    public UserSettings getOrDefault(long userId) throws SQLException {
        String sql = "SELECT allow_repeated, preferred_level FROM user_settings WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserSettings(userId, rs.getBoolean("allow_repeated"),
                            Level.fromString(rs.getString("preferred_level")));
                }
            }
        }
        return new UserSettings(userId, false, null);
    }

    public void save(UserSettings settings) throws SQLException {
        String sql = """
                INSERT INTO user_settings (user_id, allow_repeated, preferred_level)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE
                    SET allow_repeated = EXCLUDED.allow_repeated,
                        preferred_level = EXCLUDED.preferred_level
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, settings.getUserId());
            stmt.setBoolean(2, settings.isAllowRepeated());
            Level level = settings.getPreferredLevel();
            stmt.setString(3, level != null ? level.name() : null);
            stmt.executeUpdate();
        }
    }
}
