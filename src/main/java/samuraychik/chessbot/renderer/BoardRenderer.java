package samuraychik.chessbot.renderer;

public class BoardRenderer {

    public static void applyMove(char[][] board, String fromSquare, String toSquare) {
        int fromCol = fromSquare.charAt(0) - 'a';
        int fromRow = 8 - Character.getNumericValue(fromSquare.charAt(1));
        int toCol = toSquare.charAt(0) - 'a';
        int toRow = 8 - Character.getNumericValue(toSquare.charAt(1));

        board[toRow][toCol] = board[fromRow][fromCol];
        board[fromRow][fromCol] = '.';
    }

    public static String render(char[][] board) {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                sb.append(toEmoji(board[row][col], row, col));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String toEmoji(char piece, int row, int col) {
        return switch (piece) {
            case 'K' -> "🤴🏻";
            case 'Q' -> "👩🏼";
            case 'R' -> "✊🏻";
            case 'B' -> "☝🏻";
            case 'N' -> "👌🏻";
            case 'P' -> "👃🏻";
            case 'k' -> "🤴🏾";
            case 'q' -> "👩🏾";
            case 'r' -> "✊🏾";
            case 'b' -> "☝🏾";
            case 'n' -> "👌🏾";
            case 'p' -> "👃🏾";
            default -> (row + col) % 2 == 0 ? "⬜️" : "◼️";
        };
    }
}
