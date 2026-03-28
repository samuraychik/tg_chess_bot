package samuraychik.chessbot.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import samuraychik.chessbot.model.Puzzle;
import samuraychik.chessbot.model.PuzzleMove;

public class PuzzleDao {

    private final Connection connection;

    public PuzzleDao(Connection connection) {
        this.connection = connection;
    }

    public Puzzle getRandom(String difficulty) throws SQLException {
        String sql = "SELECT * FROM puzzles WHERE difficulty = ? ORDER BY RANDOM() LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, difficulty);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private Puzzle mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String difficulty = rs.getString("difficulty");
        int movesCount = rs.getInt("moves_count");
        String fen = rs.getString("fen");
        String playerColor = rs.getString("player_color");
        List<PuzzleMove> moves = parseMoves(rs.getString("moves"));
        return new Puzzle(id, name, difficulty, movesCount, fen, playerColor, moves);
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
