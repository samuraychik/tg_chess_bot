package samuraychik.chessbot.session;

import samuraychik.chessbot.model.Puzzle;

public class UserSession {

    private final long chatId;
    private SessionState state;

    private Puzzle activePuzzle;
    private int currentMoveIndex;
    private char[][] board;

    public UserSession(long chatId) {
        this.chatId = chatId;
        this.state = SessionState.IDLE;
    }

    public void startPuzzle(Puzzle puzzle, char[][] board) {
        this.activePuzzle = puzzle;
        this.board = board;
        this.currentMoveIndex = 0;
        this.state = SessionState.ACTIVE;
    }

    public void reset() {
        this.activePuzzle = null;
        this.board = null;
        this.currentMoveIndex = 0;
        this.state = SessionState.IDLE;
    }

    public long getChatId() {
        return chatId;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public Puzzle getActivePuzzle() {
        return activePuzzle;
    }

    public int getCurrentMoveIndex() {
        return currentMoveIndex;
    }

    public void incrementMoveIndex() {
        currentMoveIndex++;
    }

    public char[][] getBoard() {
        return board;
    }
}
