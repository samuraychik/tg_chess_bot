package samuraychik.chessbot.bot;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import samuraychik.chessbot.dao.PuzzleDao;
import samuraychik.chessbot.dao.UserSettingsDao;
import samuraychik.chessbot.dao.UserStatsDao;
import samuraychik.chessbot.model.Level;
import samuraychik.chessbot.model.Puzzle;
import samuraychik.chessbot.model.PuzzleMove;
import samuraychik.chessbot.model.UserSettings;
import samuraychik.chessbot.model.UserStats;
import samuraychik.chessbot.renderer.BoardRenderer;
import samuraychik.chessbot.renderer.FenParser;
import samuraychik.chessbot.session.SessionManager;
import samuraychik.chessbot.session.SessionState;
import samuraychik.chessbot.session.UserSession;

public class ChessBot extends TelegramLongPollingBot {

    private static final long BLITZ_INITIAL_MS = 120_000;
    private static final long BLITZ_BONUS_MS = 10_000;
    private static final long BLITZ_PENALTY_MS = 5_000;

    private final String botUsername;
    private final SessionManager sessionManager = new SessionManager();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final PuzzleDao puzzleDao;
    private final UserSettingsDao userSettingsDao;
    private final UserStatsDao userStatsDao;

    public ChessBot(String botToken, String botUsername, PuzzleDao puzzleDao, UserSettingsDao userSettingsDao,
            UserStatsDao userStatsDao) {
        super(botToken);
        this.botUsername = botUsername;
        this.puzzleDao = puzzleDao;
        this.userSettingsDao = userSettingsDao;
        this.userStatsDao = userStatsDao;
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
            case "BLITZ_START" -> startBlitz(chatId, session);
            case "RESET_CONFIRM" -> handleResetConfirm(chatId);
            case "SETTINGS_TOGGLE_REPEATED" -> handleToggleRepeated(chatId);
            case "SETTINGS_LEVEL_EASY", "SETTINGS_LEVEL_MEDIUM", "SETTINGS_LEVEL_HARD",
                    "SETTINGS_LEVEL_RANDOM", "SETTINGS_LEVEL_NULL" ->
                handleSetPreferredLevel(chatId, data);
            default -> handleLevelSelected(chatId, session, Level.valueOf(data));
        }
    }

    private void handleMessage(Update update) {
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        UserSession session = sessionManager.getOrCreate(chatId);

        if (session.getState() == SessionState.PUZZLE && !text.startsWith("/")) {
            handleMove(chatId, session, text);
            return;
        }
        if (session.getState() == SessionState.BLITZ && !text.startsWith("/")) {
            handleBlitzMove(chatId, session, text);
            return;
        }

        switch (text) {
            case "/start" -> sendMessage(chatId, MessageTexts.START);
            case "/puzzle" -> handlePuzzleCommand(chatId);
            case "/blitz" -> sendBlitzInfo(chatId);
            case "/skip" -> {
                if (session.getState() == SessionState.BLITZ)
                    handleBlitzSkip(chatId, session);
            }
            case "/stop" -> {
                if (session.getState() == SessionState.BLITZ)
                    endBlitz(chatId, session, "Блиц завершён досрочно.");
            }
            case "/stats" -> sendStats(chatId);
            case "/reset" -> sendResetConfirmation(chatId);
            case "/settings" -> sendSettings(chatId);
            case "/help" -> sendMessage(chatId, MessageTexts.HELP);
            case "/debug" -> sendMessage(chatId, "chatId: " + chatId + "\nstate: " + session.getState());
            default -> sendMessage(chatId, "Неизвестная команда. Список команд: /help");
        }
    }

    private void sendStats(long chatId) {
        try {
            Map<Level, Integer> stats = puzzleDao.getSolvedCountByLevel(chatId);
            int total = stats.get(Level.EASY) + stats.get(Level.MEDIUM) + stats.get(Level.HARD);
            UserStats userStats = userStatsDao.getOrDefault(chatId);
            String text = MessageTexts.STATS.formatted(
                    total, stats.get(Level.EASY), stats.get(Level.MEDIUM), stats.get(Level.HARD),
                    userStats.getBlitzHighscore());
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

    private void sendSettings(long chatId) {
        try {
            UserSettings settings = userSettingsDao.getOrDefault(chatId);
            String repeatedLabel = settings.isAllowRepeated() ? "🔁 Отключить повторы" : "🔁 Включить повторы";
            Level preferredLevel = settings.getPreferredLevel();
            String levelLabel = preferredLevel == null ? "Уровень: спрашивать каждый раз"
                    : "Уровень: " + switch (preferredLevel) {
                        case EASY -> "🟢 Лёгкий";
                        case MEDIUM -> "🟡 Средний";
                        case HARD -> "🔴 Сложный";
                        case RANDOM -> "🎲 Рандом";
                    };
            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(
                            InlineKeyboardButton.builder().text(repeatedLabel).callbackData("SETTINGS_TOGGLE_REPEATED")
                                    .build()))
                    .keyboardRow(List.of(
                            InlineKeyboardButton.builder().text("🟢").callbackData("SETTINGS_LEVEL_EASY").build(),
                            InlineKeyboardButton.builder().text("🟡").callbackData("SETTINGS_LEVEL_MEDIUM").build(),
                            InlineKeyboardButton.builder().text("🔴").callbackData("SETTINGS_LEVEL_HARD").build(),
                            InlineKeyboardButton.builder().text("🎲").callbackData("SETTINGS_LEVEL_RANDOM").build(),
                            InlineKeyboardButton.builder().text("❓").callbackData("SETTINGS_LEVEL_NULL").build()))
                    .build();
            sendMessage(chatId, MessageTexts.SETTINGS.formatted(levelLabel), keyboard);
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void handleResetConfirm(long chatId) {
        try {
            puzzleDao.resetSolved(chatId);
            sendMessage(chatId, "Прогресс сброшен.");
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void handlePuzzleCommand(long chatId) {
        UserSession session = sessionManager.getOrCreate(chatId);
        endBlitzIfRunning(chatId, session);
        try {
            UserSettings settings = userSettingsDao.getOrDefault(chatId);
            if (settings.getPreferredLevel() != null) {
                handleLevelSelected(chatId, session, settings.getPreferredLevel());
            } else {
                sendLevelKeyboard(chatId);
            }
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void endBlitzIfRunning(long chatId, UserSession session) {
        if (session.getState() == SessionState.BLITZ) {
            endBlitz(chatId, session, "Блиц завершён досрочно.");
        }
    }

    private void handleToggleRepeated(long chatId) {
        try {
            UserSettings settings = userSettingsDao.getOrDefault(chatId);
            settings.setAllowRepeated(!settings.isAllowRepeated());
            userSettingsDao.save(settings);
            sendSettings(chatId);
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void handleSetPreferredLevel(long chatId, String data) {
        try {
            UserSettings settings = userSettingsDao.getOrDefault(chatId);
            Level level = data.equals("SETTINGS_LEVEL_NULL") ? null
                    : Level.valueOf(data.replace("SETTINGS_LEVEL_", ""));
            settings.setPreferredLevel(level);
            userSettingsDao.save(settings);
            sendSettings(chatId);
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void handleLevelSelected(long chatId, UserSession session, Level level) {
        try {
            UserSettings settings = userSettingsDao.getOrDefault(chatId);
            boolean allowRepeated = settings.isAllowRepeated();
            Puzzle puzzle = allowRepeated ? puzzleDao.getRandom(level) : puzzleDao.getRandom(level, chatId);
            if (puzzle == null) {
                sendMessage(chatId, "Не удалось найти задачу.");
                return;
            }
            char[][] board = FenParser.parse(puzzle.getFen());
            session.startPuzzle(puzzle, board);
            sendMessage(chatId, buildPuzzleMessage(puzzle, board));
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void sendBlitzInfo(long chatId) {
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("⚡ Начать!").callbackData("BLITZ_START").build()))
                .build();
        sendMessage(chatId, MessageTexts.BLITZ, keyboard);
    }

    private void startBlitz(long chatId, UserSession session) {
        endBlitzIfRunning(chatId, session);
        try {
            long endTime = System.currentTimeMillis() + BLITZ_INITIAL_MS;
            session.startBlitz(endTime);
            Puzzle puzzle = puzzleDao.getRandomForBlitz(getBlitzLevel(0), session.getBlitzUsedIds());
            if (puzzle == null) {
                sendMessage(chatId, "Не удалось найти задачу — завершаю сеанс.");
                session.resetBlitz();
                return;
            }
            char[][] board = FenParser.parse(puzzle.getFen());
            session.startPuzzle(puzzle, board);
            session.getBlitzUsedIds().add(puzzle.getId());
            session.setState(SessionState.BLITZ);
            scheduleBlitzTasks(chatId, session);
            sendMessage(chatId, buildBlitzPuzzleMessage(puzzle, board, session));
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void handleBlitzMove(long chatId, UserSession session, String input) {
        Puzzle puzzle = session.getActivePuzzle();
        char[][] board = session.getBoard();
        PuzzleMove expected = puzzle.getMoves().get(session.getCurrentMoveIndex());

        if (!input.trim().equalsIgnoreCase(expected.getNotation())) {
            session.setBlitzEndTime(session.getBlitzEndTime() - BLITZ_PENALTY_MS);
            scheduleBlitzTasks(chatId, session);
            sendMessage(chatId, "❌ Неверный ход, -5 секунд\n\n"
                    + formatRemaining(session.getBlitzRemainingMs()));
            return;
        }

        BoardRenderer.applyMove(board, expected.getFromSquare(), expected.getToSquare());
        session.incrementMoveIndex();

        if (session.getCurrentMoveIndex() < puzzle.getMoves().size()) {
            PuzzleMove response = puzzle.getMoves().get(session.getCurrentMoveIndex());
            BoardRenderer.applyMove(board, response.getFromSquare(), response.getToSquare());
            session.incrementMoveIndex();
            sendMessage(chatId, response.getNotation() + "\n\n" + BoardRenderer.render(board) + "\n\n"
                    + formatRemaining(session.getBlitzRemainingMs()));
            return;
        }

        session.incrementBlitzSolved();
        session.setBlitzEndTime(session.getBlitzEndTime() + BLITZ_BONUS_MS);
        scheduleBlitzTasks(chatId, session);

        sendMessage(chatId, "✅ Задача решена! +10 секунд\n\n" + BoardRenderer.render(board) + "\n\n"
                + formatRemaining(session.getBlitzRemainingMs()));

        sendNextBlitzPuzzle(chatId, session);
    }

    private void handleBlitzSkip(long chatId, UserSession session) {
        session.setBlitzEndTime(session.getBlitzEndTime() - BLITZ_PENALTY_MS);
        scheduleBlitzTasks(chatId, session);
        sendMessage(chatId, "⏭ Пропуск задачи, -5 секунд\n\n"
                + formatRemaining(session.getBlitzRemainingMs()));
        sendNextBlitzPuzzle(chatId, session);
    }

    private void sendNextBlitzPuzzle(long chatId, UserSession session) {
        try {
            Puzzle next = puzzleDao.getRandomForBlitz(getBlitzLevel(session.getBlitzSolved()),
                    session.getBlitzUsedIds());
            if (next == null) {
                endBlitz(chatId, session, "Задачи закончились.");
                return;
            }
            char[][] nextBoard = FenParser.parse(next.getFen());
            session.startPuzzle(next, nextBoard);
            session.getBlitzUsedIds().add(next.getId());
            session.setState(SessionState.BLITZ);
            sendMessage(chatId, buildBlitzPuzzleMessage(next, nextBoard, session));
        } catch (SQLException e) {
            System.err.println(e);
            endBlitz(chatId, session, "Произошла ошибка.");
        }
    }

    private void scheduleBlitzTasks(long chatId, UserSession session) {
        session.cancelBlitzTasks();
        long remaining = session.getBlitzRemainingMs();
        if (remaining <= 0) {
            endBlitz(chatId, session, "Время вышло!");
            return;
        }

        ScheduledFuture<?> endTask = scheduler.schedule(
                () -> endBlitz(chatId, session, "Время вышло!"),
                remaining, TimeUnit.MILLISECONDS);
        session.setBlitzEndTask(endTask);

        long[] warnDelays = { remaining - 60_000, remaining - 30_000, remaining - 10_000 };
        String[] warnMessages = { "⏰ Осталась 1 минута!", "⏰ Осталось 30 секунд!", "⏰ Осталось 10 секунд!" };
        ScheduledFuture<?>[] warnTasks = new ScheduledFuture<?>[3];
        for (int i = 0; i < 3; i++) {
            if (warnDelays[i] > 0) {
                final String msg = warnMessages[i];
                warnTasks[i] = scheduler.schedule(() -> sendMessage(chatId, msg), warnDelays[i], TimeUnit.MILLISECONDS);
            }
        }
        session.setBlitzWarnTasks(warnTasks);
    }

    private void endBlitz(long chatId, UserSession session, String reason) {
        if (session.getState() != SessionState.BLITZ)
            return;
        int solved = session.getBlitzSolved();
        session.resetBlitz();
        try {
            userStatsDao.updateHighscore(chatId, solved);
            UserStats stats = userStatsDao.getOrDefault(chatId);
            sendMessage(chatId, reason + "\n\n⚡ Блиц завершён!\n✅ Решено задач: " + solved
                    + "\n🏆 Рекорд: " + stats.getBlitzHighscore());
        } catch (SQLException e) {
            System.err.println(e);
            sendMessage(chatId, reason + "\n\n⚡ Блиц завершён! Решено задач: " + solved);
        }
    }

    private String buildBlitzPuzzleMessage(Puzzle puzzle, char[][] board, UserSession session) {
        return "⚡ Счёт: " + session.getBlitzSolved() + "\n\n"
                + buildPuzzleMessage(puzzle, board) + "\n"
                + formatRemaining(session.getBlitzRemainingMs());
    }

    private String formatRemaining(long ms) {
        long secs = Math.max(ms, 0) / 1000;
        return String.format("⏱ %d:%02d", secs / 60, secs % 60);
    }

    private Level getBlitzLevel(int solved) {
        if (solved < 8)
            return Level.EASY;
        if (solved < 17)
            return Level.MEDIUM;
        return Level.HARD;
    }

    private void handleMove(long chatId, UserSession session, String input) {
        Puzzle puzzle = session.getActivePuzzle();
        char[][] board = session.getBoard();
        PuzzleMove expected = puzzle.getMoves().get(session.getCurrentMoveIndex());

        if (!input.trim().equalsIgnoreCase(expected.getNotation())) {
            sendMessage(chatId, "❌ Неверный ход, попробуй ещё раз");
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
            sendMessage(chatId, "✅ Задача решена!\n\n" + BoardRenderer.render(board));
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
            case RANDOM -> "";
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
