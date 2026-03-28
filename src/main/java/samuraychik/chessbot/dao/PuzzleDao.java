package samuraychik.chessbot.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import samuraychik.chessbot.model.Level;
import samuraychik.chessbot.model.Puzzle;
import samuraychik.chessbot.model.PuzzleMove;

public class PuzzleDao {

    private final Connection connection;

    public PuzzleDao(Connection connection) {
        this.connection = connection;
    }

    public Puzzle getRandom(Level level, long userId) throws SQLException {
        String sql = """
                SELECT * FROM puzzles
                WHERE level = ?
                AND id NOT IN (SELECT puzzle_id FROM user_solved_puzzles WHERE user_id = ?)
                ORDER BY RANDOM() LIMIT 1
                    """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, level.name());
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Map<Level, Integer> getSolvedCountByLevel(long userId) throws SQLException {
        String sql = """
                SELECT p.level, COUNT(*) AS count
                FROM user_solved_puzzles usp
                JOIN puzzles p ON p.id = usp.puzzle_id
                WHERE usp.user_id = ?
                GROUP BY p.level
                """;
        Map<Level, Integer> result = new LinkedHashMap<>();
        for (Level level : Level.values()) {
            result.put(level, 0);
        }
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.put(Level.valueOf(rs.getString("level")), rs.getInt("count"));
                }
            }
        }
        return result;
    }

    private Puzzle mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        Level level = Level.valueOf(rs.getString("level"));
        int movesCount = rs.getInt("moves_count");
        String fen = rs.getString("fen");
        String playerColor = rs.getString("player_color");
        List<PuzzleMove> moves = parseMoves(rs.getString("moves"));
        return new Puzzle(id, name, level, movesCount, fen, playerColor, moves);
    }

    private List<PuzzleMove> parseMoves(String moves) {
        List<PuzzleMove> parsedMoves = new ArrayList<>();
        for (String token : moves.split(",")) {
            String[] parts = token.split("_");
            String piece = parts[0];
            String[] squares = parts[1].split("-");
            parsedMoves.add(new PuzzleMove(piece, squares[0], squares[1]));
        }
        return parsedMoves;
    }
}
