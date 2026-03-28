package samuraychik.chessbot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import samuraychik.chessbot.bot.ChessBot;

public class Main {
    public static void main(String[] args) {
        Properties config = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("где config.properties???");
                return;
            }
            config.load(input);
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        String token = config.getProperty("bot.token");
        String username = config.getProperty("bot.username");

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ChessBot(token, username));
            System.out.println("бот запущен!!!");
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }
}
