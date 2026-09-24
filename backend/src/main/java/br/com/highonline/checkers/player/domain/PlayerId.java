package br.com.highonline.checkers.player.domain;

import java.util.UUID;

public record PlayerId(UUID value) {
    public PlayerId {
        if (value == null) throw new IllegalArgumentException("Player ID is required");
    }

    public static PlayerId newId() {
        return new PlayerId(UUID.randomUUID());
    }
}
