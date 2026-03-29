CREATE TABLE
    IF NOT EXISTS puzzles (
        id SERIAL PRIMARY KEY,
        name TEXT,
        level TEXT NOT NULL CHECK (level IN ('EASY', 'MEDIUM', 'HARD')),
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

CREATE TABLE
    IF NOT EXISTS user_stats (
        user_id BIGINT PRIMARY KEY,
        blitz_highscore INT NOT NULL DEFAULT 0
    );

CREATE TABLE
    IF NOT EXISTS user_settings (
        user_id BIGINT PRIMARY KEY,
        allow_repeated BOOLEAN NOT NULL DEFAULT false,
        preferred_level TEXT DEFAULT NULL CHECK (
            preferred_level IN ('EASY', 'MEDIUM', 'HARD', 'RANDOM')
        )
    );