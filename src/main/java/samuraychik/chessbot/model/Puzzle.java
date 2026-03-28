package samuraychik.chessbot.model;

import java.util.List;

public class Puzzle {

    private final int id;
    private final String name;
    private final String difficulty;
    private final int movesCount;
    private final String fen;
    private final String playerColor;
    private final List<PuzzleMove> moves;

    public Puzzle(int id, String name, String difficulty, int movesCount, String fen, String playerColor,
            List<PuzzleMove> moves) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.movesCount = movesCount;
        this.fen = fen;
        this.playerColor = playerColor;
        this.moves = moves;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDifficulty() {
        return difficulty;
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
