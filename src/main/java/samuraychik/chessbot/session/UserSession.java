package samuraychik.chessbot.session;

public class UserSession {

    private final long chatId;
    private SessionState state;

    public UserSession(long chatId) {
        this.chatId = chatId;
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

    public void reset() {
        this.state = SessionState.IDLE;
    }
}
