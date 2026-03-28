package samuraychik.chessbot.bot;

public class MessageTexts {

    public static final String START = """
            Привет! Я ботик шахматный задачник — потренируй свою тактику 🏆

            Начни решать с /puzzle, подробности в /help.
            """;

    public static final String HELP = """
            /puzzle — случайная задача
            /stats — твоя статистика
            /help — это сообщение

            Ходы вводятся в стандартной нотации:
              e4 — пешка на e4
              Nf3 — конь на f3

            Фигуры:
            🤴🏻🤴🏾 Король  K
            👩🏼👩🏾 Ферзь   Q
            ✊🏻✊🏾 Ладья   R
            ☝🏻☝🏾 Слон    B
            👌🏻👌🏾 Конь    N
            👃🏻👃🏾 Пешка   (без буквы)
            """;

    private MessageTexts() {
    }
}
