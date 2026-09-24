package br.com.highonline.checkers.game.domain;

public record Square(int row, int column) {
    public Square {
        if (row < 0 || row > 7 || column < 0 || column > 7) {
            throw new IllegalArgumentException("Square must be inside an 8x8 board");
        }
    }

    public boolean isDark() {
        return (row + column) % 2 == 1;
    }
}
