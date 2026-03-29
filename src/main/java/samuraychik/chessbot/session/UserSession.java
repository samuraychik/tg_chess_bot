package samuraychik.chessbot.session;

import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;

import samuraychik.chessbot.model.Puzzle;

public class UserSession {

    private final long chatId;
    private SessionState state;

    private Puzzle activePuzzle;
    private int currentMoveIndex;
    private char[][] board;

    private int blitzSolved;
    private long blitzEndTime;
    private HashSet<Integer> blitzUsedIds;
    private ScheduledFuture<?> blitzEndTask;
    private ScheduledFuture<?>[] blitzWarnTasks;

    public UserSession(long chatId) {
        this.chatId = chatId;
        this.state = SessionState.IDLE;
    }

    public void startPuzzle(Puzzle puzzle, char[][] board) {
        this.activePuzzle = puzzle;
        this.board = board;
        this.currentMoveIndex = 0;
        this.state = SessionState.PUZZLE;
    }

    public void startBlitz(long endTime) {
        this.blitzSolved = 0;
        this.blitzEndTime = endTime;
        this.blitzUsedIds = new HashSet<>();
        this.blitzEndTask = null;
        this.blitzWarnTasks = new ScheduledFuture<?>[3];
        this.state = SessionState.BLITZ;
    }

    public void resetBlitz() {
        cancelBlitzTasks();
        this.blitzSolved = 0;
        this.blitzEndTime = 0;
        this.blitzUsedIds = null;
        this.blitzEndTask = null;
        this.blitzWarnTasks = null;
        this.activePuzzle = null;
        this.board = null;
        this.currentMoveIndex = 0;
        this.state = SessionState.IDLE;
    }

    public void cancelBlitzTasks() {
        if (blitzEndTask != null)
            blitzEndTask.cancel(false);
        if (blitzWarnTasks != null) {
            for (ScheduledFuture<?> task : blitzWarnTasks) {
                if (task != null)
                    task.cancel(false);
            }
        }
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

    public int getBlitzSolved() {
        return blitzSolved;
    }

    public long getBlitzEndTime() {
        return blitzEndTime;
    }

    public void setBlitzEndTime(long blitzEndTime) {
        this.blitzEndTime = blitzEndTime;
    }

    public void incrementBlitzSolved() {
        blitzSolved++;
    }

    public HashSet<Integer> getBlitzUsedIds() {
        return blitzUsedIds;
    }

    public ScheduledFuture<?> getBlitzEndTask() {
        return blitzEndTask;
    }

    public void setBlitzEndTask(ScheduledFuture<?> blitzEndTask) {
        this.blitzEndTask = blitzEndTask;
    }

    public ScheduledFuture<?>[] getBlitzWarnTasks() {
        return blitzWarnTasks;
    }

    public void setBlitzWarnTasks(ScheduledFuture<?>[] blitzWarnTasks) {
        this.blitzWarnTasks = blitzWarnTasks;
    }
}
