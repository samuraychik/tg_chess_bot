package samuraychik.chessbot.renderer;

public class FenParser {

    public static char[][] parse(String fen) {
        char[][] board = new char[8][8];
        String position = fen.split(" ")[0];
        String[] rows = position.split("/");

        for (int row = 0; row < 8; row++) {
            int col = 0;
            for (char c : rows[row].toCharArray()) {
                if (Character.isDigit(c)) {
                    int emptyCount = Character.getNumericValue(c);
                    for (int i = 0; i < emptyCount; i++) {
                        board[row][col++] = '.';
                    }
                } else {
                    board[row][col++] = c;
                }
            }
        }
        return board;
    }
}
