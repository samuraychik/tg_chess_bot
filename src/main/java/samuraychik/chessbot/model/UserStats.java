package samuraychik.chessbot.model;

public class UserStats {

    private final long userId;
    private int blitzHighscore;

    public UserStats(long userId, int blitzHighscore) {
        this.userId = userId;
        this.blitzHighscore = blitzHighscore;
    }

    public long getUserId() {
        return userId;
    }

    public int getBlitzHighscore() {
        return blitzHighscore;
    }

    public void setBlitzHighscore(int blitzHighscore) {
        this.blitzHighscore = blitzHighscore;
    }
}
