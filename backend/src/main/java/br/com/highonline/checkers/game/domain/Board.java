package br.com.highonline.checkers.game.domain;

import java.util.Map;
import java.util.HashMap;

public final class Board {
    private final Map<Square, Piece> pieces;

    private Board(Map<Square, Piece> pieces) {
        this.pieces = Map.copyOf(pieces);
    }

    // Package-private position factory for rules and domain scenarios.
    static Board of(Map<Square, Piece> pieces) {
        if (pieces == null || pieces.keySet().stream().anyMatch(square -> !square.isDark())) {
            throw new IllegalArgumentException("Pieces must occupy dark squares");
        }
        return new Board(pieces);
    }

    Map<Square, Piece> pieces() {
        return pieces;
    }

    public static Board initial() {
        Map<Square, Piece> pieces = new HashMap<>();
        for (int row = 0; row < 8; row++) {
            if (row >= 3 && row <= 4) continue;
            Color color = row < 3 ? Color.BLACK : Color.WHITE;
            for (int column = 0; column < 8; column++) {
                Square square = new Square(row, column);
                if (square.isDark()) pieces.put(square, new Piece(color, false));
            }
        }
        return new Board(pieces);
    }

    public Piece pieceAt(Square square) {
        if (square == null) throw new IllegalArgumentException("Square is required");
        return pieces.get(square);
    }

    public long count(Color color) {
        if (color == null) throw new IllegalArgumentException("Color is required");
        return pieces.values().stream().filter(piece -> piece.color() == color).count();
    }
}

