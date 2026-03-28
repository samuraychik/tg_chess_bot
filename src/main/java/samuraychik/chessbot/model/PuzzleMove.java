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
        char fromFile = fromSquare.charAt(0);
        char toFile = toSquare.charAt(0);

        if (piece.equals("K")) {
            if (fromFile == 'e' && toFile == 'g') return "O-O";
            if (fromFile == 'e' && toFile == 'c') return "O-O-O";
        }
        if (piece.equals("P") && fromFile != toFile) {
            return "" + fromFile + toFile;
        }
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

    @Override
    public String toString() {
        return piece + fromSquare + "-" + toSquare;
    }
}
