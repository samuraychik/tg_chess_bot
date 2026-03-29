package samuraychik.chessbot.model;

public enum Level {
    EASY, MEDIUM, HARD, RANDOM;

    public static Level fromString(String s) {
        return s != null ? Level.valueOf(s) : null;
    }
}
