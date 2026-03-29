\encoding UTF8

INSERT INTO puzzles (name, level, moves_count, fen, player_color, moves)
VALUES
    (
        'дурацццкий мат',
        'EASY', 1,
        'r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w - - 0 1',
        'WHITE',
        'Q_h5-f7'
    ),
    (
        'матик ферзём',
        'MEDIUM', 1,
        '2rk4/8/4K3/8/8/Q7/8/8 w - - 0 1',
        'WHITE',
        'Q_a3-e7'
    ),
    (
        '',
        'EASY', 1,
        'rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 1',
        'BLACK',
        'Q_d8-h4'
    ),
    (
        'типикал мат по последней горизонтали',
        'EASY', 2,
        '6k1/5ppp/8/8/8/8/3r1PPP/1R4K1 w - - 0 1',
        'WHITE',
        'R_b1-b8,R_d2-d8,R_b8-d8'
    ),
    (
        'НЕВОЗМОЖНАЯ ЗАДАЧА',
        'HARD', 1,
        '8/8/8/8/8/8/1R6/R3K2k w Q - 0 1',
        'WHITE',
        'K_e1-c1'
    );

