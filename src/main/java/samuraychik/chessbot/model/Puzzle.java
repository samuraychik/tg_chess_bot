package samuraychik.chessbot.model;

import java.util.List;

public class Puzzle {

    private final int id;
    private final Level level;
    private final int movesCount;
    private final String fen;
    private final String playerColor;
    private final List<PuzzleMove> moves;

    public Puzzle(int id, Level level, int movesCount, String fen, String playerColor, List<PuzzleMove> moves) {
        this.id = id;
        this.level = level;
        this.movesCount = movesCount;
        this.fen = fen;
        this.playerColor = playerColor;
        this.moves = moves;
    }

    public int getId() {
        return id;
    }

    public Level getLevel() {
        return level;
    }

    public int getMovesCount() {
        return movesCount;
    }

    public String getFen() {
        return fen;
    }

    public String getPlayerColor() {
        return playerColor;
    }

    public List<PuzzleMove> getMoves() {
        return moves;
    }
}
