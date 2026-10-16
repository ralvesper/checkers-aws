package br.com.highonline.checkers.game.domain;

import java.util.List;

/** One complete turn, including every landing of a multiple capture. */
public record Move(Square from, List<Square> landings) {
    public Move {
        if (from == null || landings == null || landings.isEmpty()
                || landings.stream().anyMatch(square -> square == null)) {
            throw new IllegalArgumentException("Origin and at least one landing are required");
        }
        landings = List.copyOf(landings);
        if (!from.isDark() || landings.stream().anyMatch(square -> !square.isDark())) {
            throw new IllegalArgumentException("Moves must use dark squares");
        }
    }
}
