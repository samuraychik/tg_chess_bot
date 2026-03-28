CREATE TABLE
    IF NOT EXISTS puzzles (
        id SERIAL PRIMARY KEY,
        name TEXT,
        difficulty TEXT NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
        moves_count INT NOT NULL,
        fen TEXT NOT NULL,
        player_color TEXT NOT NULL CHECK (player_color IN ('WHITE', 'BLACK')),
        moves TEXT NOT NULL
    );

CREATE TABLE
    IF NOT EXISTS user_solved_puzzles (
        user_id BIGINT NOT NULL,
        puzzle_id INT NOT NULL REFERENCES puzzles (id),
        PRIMARY KEY (user_id, puzzle_id)
    );