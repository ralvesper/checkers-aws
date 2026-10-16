package br.com.highonline.checkers.game.domain;

public final class StaleGameVersionException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public StaleGameVersionException() {
        super("Game version does not match the expected version");
    }
}
