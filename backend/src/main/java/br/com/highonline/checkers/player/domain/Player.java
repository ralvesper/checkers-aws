package br.com.highonline.checkers.player.domain;

import java.util.Objects;

/** Player aggregate. Authentication and profile changes belong to this context. */
public final class Player {
    private final PlayerId id;
    private final String displayName;

    private Player(PlayerId id, String displayName) {
        this.id = Objects.requireNonNull(id);
        this.displayName = displayName;
    }

    public static Player register(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.strip().length() > 40) {
            throw new IllegalArgumentException("Display name must contain 1 to 40 characters");
        }
        return new Player(PlayerId.newId(), displayName.strip());
    }

    public PlayerId id() { return id; }
    public String displayName() { return displayName; }
}
