package samuraychik.chessbot.model;

public class UserSettings {

    private final long userId;
    private boolean allowRepeated;
    private Level preferredLevel;

    public UserSettings(long userId, boolean allowRepeated, Level preferredLevel) {
        this.userId = userId;
        this.allowRepeated = allowRepeated;
        this.preferredLevel = preferredLevel;
    }

    public long getUserId() {
        return userId;
    }

    public boolean isAllowRepeated() {
        return allowRepeated;
    }

    public void setAllowRepeated(boolean allowRepeated) {
        this.allowRepeated = allowRepeated;
    }

    public Level getPreferredLevel() {
        return preferredLevel;
    }

    public void setPreferredLevel(Level preferredLevel) {
        this.preferredLevel = preferredLevel;
    }
}
