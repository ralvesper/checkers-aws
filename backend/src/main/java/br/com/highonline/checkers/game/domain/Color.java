package br.com.highonline.checkers.game.domain;

public enum Color {
    BLACK, WHITE;

    public Color opposite() {
        return this == BLACK ? WHITE : BLACK;
    }
}
