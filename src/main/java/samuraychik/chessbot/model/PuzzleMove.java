package samuraychik.chessbot.model;

public class PuzzleMove {

    private final String piece;
    private final String fromSquare;
    private final String toSquare;

    public PuzzleMove(String piece, String fromSquare, String toSquare) {
        this.piece = piece;
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
    }

    public String getNotation() {
        return piece.equals("P") ? toSquare : piece + toSquare;
    }

    public String getPiece() {
        return piece;
    }

    public String getFromSquare() {
        return fromSquare;
    }

    public String getToSquare() {
        return toSquare;
    }
}
