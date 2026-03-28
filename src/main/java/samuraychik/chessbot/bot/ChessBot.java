package samuraychik.chessbot.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import samuraychik.chessbot.session.SessionManager;
import samuraychik.chessbot.session.UserSession;

public class ChessBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final SessionManager sessionManager = new SessionManager();

    public ChessBot(String botToken, String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText())
            return;

        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        UserSession session = sessionManager.getOrCreate(chatId);

        if (text.equals("/start")) {
            sendMessage(chatId, "команда старт!!!");
        } else if (text.equals("/debug")) {
            sendMessage(chatId, "chatId: " + chatId + "\nstate: " + session.getState());
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
