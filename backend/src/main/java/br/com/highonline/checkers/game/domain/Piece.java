package br.com.highonline.checkers.game.domain;

public record Piece(Color color, boolean king) {
    public Piece {
        if (color == null) throw new IllegalArgumentException("Piece color is required");
    }
}
