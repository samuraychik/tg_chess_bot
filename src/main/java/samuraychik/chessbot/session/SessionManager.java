package samuraychik.chessbot.session;

import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getOrCreate(long chatId) {
        return sessions.computeIfAbsent(chatId, UserSession::new);
    }

    public void remove(long chatId) {
        sessions.remove(chatId);
    }
}
