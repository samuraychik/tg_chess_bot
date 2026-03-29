package samuraychik.chessbot.bot;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import samuraychik.chessbot.dao.PuzzleDao;
import samuraychik.chessbot.model.Level;
import samuraychik.chessbot.model.Puzzle;
import samuraychik.chessbot.model.PuzzleMove;
import samuraychik.chessbot.renderer.BoardRenderer;
import samuraychik.chessbot.renderer.FenParser;
import samuraychik.chessbot.session.SessionManager;
import samuraychik.chessbot.session.SessionState;
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
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        }
    }

    private void handleCallbackQuery(Update update) {
        answerCallback(update.getCallbackQuery().getId());

        String data = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        UserSession session = sessionManager.getOrCreate(chatId);

        switch (data) {
            case "RESET_CONFIRM" -> handleResetConfirm(chatId);
            default -> handleLevelSelected(chatId, session, data);
        }
    }

    private void handleMessage(Update update) {
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        UserSession session = sessionManager.getOrCreate(chatId);

        if (session.getState() == SessionState.ACTIVE && !text.startsWith("/")) {
            handleMove(chatId, session, text);
            return;
        }

        switch (text) {
            case "/start" -> sendMessage(chatId, MessageTexts.START);
            case "/help" -> sendMessage(chatId, MessageTexts.HELP);
            case "/debug" -> sendMessage(chatId, "chatId: " + chatId + "\nstate: " + session.getState());
            case "/stats" -> sendStats(chatId);
            case "/puzzle" -> sendLevelKeyboard(chatId);
            case "/reset" -> sendResetConfirmation(chatId);
            default -> {
            }
        }
    }

    private void sendStats(long chatId) {
        try {
            Map<Level, Integer> stats = puzzleDao.getSolvedCountByLevel(chatId);
            int total = stats.get(Level.EASY) + stats.get(Level.MEDIUM) + stats.get(Level.HARD);
            String text = String.format(
                    "Решено задач: %d\n\n🟢 Лёгкие: %d\n🟡 Средние: %d\n🔴 Сложные: %d",
                    total, stats.get(Level.EASY), stats.get(Level.MEDIUM), stats.get(Level.HARD));
            sendMessage(chatId, text);
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void sendLevelKeyboard(long chatId) {
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("🟢 Лёгкий").callbackData("EASY").build(),
                        InlineKeyboardButton.builder().text("🟡 Средний").callbackData("MEDIUM").build(),
                        InlineKeyboardButton.builder().text("🔴 Сложный").callbackData("HARD").build()))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("🎲 Рандом").callbackData("RANDOM").build()))
                .build();
        sendMessage(chatId, "Выбери уровень:", keyboard);
    }

    private void sendResetConfirmation(long chatId) {
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("Да, сбросить").callbackData("RESET_CONFIRM").build()))
                .build();
        sendMessage(chatId, "⚠️ Весь прогресс будет удалён. Ты уверен?", keyboard);
    }

    private void handleResetConfirm(long chatId) {
        try {
            puzzleDao.resetSolved(chatId);
            sendMessage(chatId, "Прогресс сброшен.");
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void handleLevelSelected(long chatId, UserSession session, String data) {
        try {
            Puzzle puzzle = data.equals("RANDOM")
                    ? puzzleDao.getRandom(chatId)
                    : puzzleDao.getRandom(Level.valueOf(data), chatId);
            if (puzzle == null) {
                sendMessage(chatId, "Ты решил все задачи этого уровня!");
                return;
            }
            char[][] board = FenParser.parse(puzzle.getFen());
            session.startPuzzle(puzzle, board);
            sendMessage(chatId, buildPuzzleMessage(puzzle, board));
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void handleMove(long chatId, UserSession session, String input) {
        Puzzle puzzle = session.getActivePuzzle();
        char[][] board = session.getBoard();
        PuzzleMove expected = puzzle.getMoves().get(session.getCurrentMoveIndex());

        if (!input.trim().equalsIgnoreCase(expected.getNotation())) {
            sendMessage(chatId, "Неверный ход, попробуй ещё раз");
            return;
        }

        BoardRenderer.applyMove(board, expected.getFromSquare(), expected.getToSquare());
        session.incrementMoveIndex();

        if (session.getCurrentMoveIndex() >= puzzle.getMoves().size()) {
            try {
                puzzleDao.recordSolved(chatId, puzzle.getId());
            } catch (SQLException e) {
                System.err.println(e);
            }
            sendMessage(chatId, "Задача решена! 🎉\n\n" + BoardRenderer.render(board));
            session.reset();
            return;
        }

        PuzzleMove response = puzzle.getMoves().get(session.getCurrentMoveIndex());
        BoardRenderer.applyMove(board, response.getFromSquare(), response.getToSquare());
        session.incrementMoveIndex();
        sendMessage(chatId, response.getNotation() + "\n\n" + BoardRenderer.render(board));
    }

    private String buildPuzzleMessage(Puzzle puzzle, char[][] board) {
        String level = switch (puzzle.getLevel()) {
            case EASY -> "🟢 Легко";
            case MEDIUM -> "🟡 Средне";
            case HARD -> "🔴 Сложно";
        };
        String color = puzzle.getPlayerColor().equals("WHITE") ? "⬜" : "⬛";
        String task = color + " Мат в " + puzzle.getMovesCount() + " " + movesWord(puzzle.getMovesCount());
        String name = puzzle.getName();
        String header = level + "\n" + ((name != null && !name.isBlank()) ? name + "\n" + task : task);
        return header + "\n\n" + BoardRenderer.render(board);
    }

    private String movesWord(int n) {
        int lastTwo = n % 100;
        int lastOne = n % 10;
        if (lastTwo >= 11 && lastTwo <= 19)
            return "ходов";
        if (lastOne == 1)
            return "ход";
        if (lastOne >= 2 && lastOne <= 4)
            return "хода";
        return "ходов";
    }

    private void sendMessage(long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    private void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboard != null)
            message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }

    private void answerCallback(String callbackId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }
}
