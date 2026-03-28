package samuraychik.chessbot.bot;

import java.sql.SQLException;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import samuraychik.chessbot.dao.PuzzleDao;
import samuraychik.chessbot.model.Puzzle;
import samuraychik.chessbot.session.SessionManager;
import samuraychik.chessbot.session.UserSession;

public class ChessBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final SessionManager sessionManager = new SessionManager();
    private final PuzzleDao puzzleDao;

    public ChessBot(String botToken, String botUsername, PuzzleDao puzzleDao) {
        super(botToken);
        this.botUsername = botUsername;
        this.puzzleDao = puzzleDao;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText())
            return;

        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        UserSession session = sessionManager.getOrCreate(chatId);

        switch (text) {
            case "/start" -> sendMessage(chatId, "команда старт!!!");
            case "/debug" -> sendMessage(chatId, "chatId: " + chatId + "\nstate: " + session.getState());
            case "/task" -> {
                try {
                    Puzzle puzzle = puzzleDao.getRandom("EASY");
                    if (puzzle == null) {
                        sendMessage(chatId, "нет задач(");
                        return;
                    }
                    String dump = "id: " + puzzle.getId()
                            + "\nname: " + puzzle.getName()
                            + "\ndifficulty: " + puzzle.getDifficulty()
                            + "\nmoves_count: " + puzzle.getMovesCount()
                            + "\nplayer_color: " + puzzle.getPlayerColor()
                            + "\nfen: " + puzzle.getFen()
                            + "\nmoves: " + puzzle.getMoves();
                    sendMessage(chatId, dump);
                } catch (SQLException e) {
                    System.err.println(e);
                }
            }
            default -> {
            }
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
