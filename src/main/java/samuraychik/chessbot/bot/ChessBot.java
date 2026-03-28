package samuraychik.chessbot.bot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
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
    private final Connection dbConnection;

    public ChessBot(String botToken, String botUsername, PuzzleDao puzzleDao, Connection dbConnection) {
        super(botToken);
        this.botUsername = botUsername;
        this.puzzleDao = puzzleDao;
        this.dbConnection = dbConnection;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText())
            return;

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
            case "/puzzle" -> startPuzzle(chatId, session);
            case "/stats" -> sendStats(chatId);
            default -> {
            }
        }
    }

    private void sendStats(long chatId) {
        try {
            Map<Level, Integer> stats = puzzleDao.getSolvedCountByLevel(chatId);
            int total = stats.get(Level.EASY) + stats.get(Level.MEDIUM) + stats.get(Level.HARD);
            String text = String.format(
                    "Решено задач: %d\n🟢 Easy: %d\n🟡 Medium: %d\n🔴 Hard: %d",
                    total, stats.get(Level.EASY), stats.get(Level.MEDIUM), stats.get(Level.HARD));
            sendMessage(chatId, text);
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    private void startPuzzle(long chatId, UserSession session) {
        try {
            Puzzle puzzle = puzzleDao.getRandom(Level.EASY, chatId);
            if (puzzle == null) {
                sendMessage(chatId, "нет задач(");
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
            recordSolvedPuzzle(chatId, puzzle.getId());
            sendMessage(chatId, "Задача решена! 🎉\n\n" + BoardRenderer.render(board));
            session.reset();
            return;
        }

        PuzzleMove response = puzzle.getMoves().get(session.getCurrentMoveIndex());
        BoardRenderer.applyMove(board, response.getFromSquare(), response.getToSquare());
        session.incrementMoveIndex();
        sendMessage(chatId, response.getNotation() + "\n\n"
                + BoardRenderer.render(board));
    }

    private String buildPuzzleMessage(Puzzle puzzle, char[][] board) {
        String color = puzzle.getPlayerColor().equals("WHITE") ? "⬜" : "⬛";
        String task = color + " Мат в " + puzzle.getMovesCount() + " " + movesWord(puzzle.getMovesCount());
        String name = puzzle.getName();
        String header = (name != null && !name.isBlank()) ? name + "\n" + task : task;
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

    private void recordSolvedPuzzle(long userId, int puzzleId) {
        String sql = "INSERT INTO user_solved_puzzles (user_id, puzzle_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, puzzleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e);
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
