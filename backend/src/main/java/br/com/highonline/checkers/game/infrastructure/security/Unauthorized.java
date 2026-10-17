package br.com.highonline.checkers.game.infrastructure.security;

public final class Unauthorized extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public Unauthorized() { super("Missing, invalid or expired session token"); }
}
