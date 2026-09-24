package br.com.highonline.checkers.game.domain;

import java.util.UUID;

public record GameId(UUID value) {
    public GameId {
        if (value == null) throw new IllegalArgumentException("Game ID is required");
    }

    public static GameId newId() {
        return new GameId(UUID.randomUUID());
    }
}
