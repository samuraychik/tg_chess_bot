package samuraychik.chessbot.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import samuraychik.chessbot.model.UserStats;

public class UserStatsDao {

    private final Connection connection;

    public UserStatsDao(Connection connection) {
        this.connection = connection;
    }

    public UserStats getOrDefault(long userId) throws SQLException {
        String sql = "SELECT blitz_highscore FROM user_stats WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserStats(userId, rs.getInt("blitz_highscore"));
                }
            }
        }
        return new UserStats(userId, 0);
    }

    public void updateHighscore(long userId, int score) throws SQLException {
        String sql = """
                INSERT INTO user_stats (user_id, blitz_highscore)
                VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE
                    SET blitz_highscore = EXCLUDED.blitz_highscore
                WHERE user_stats.blitz_highscore < EXCLUDED.blitz_highscore
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, score);
            stmt.executeUpdate();
        }
    }
}
