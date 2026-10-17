package br.com.highonline.checkers.game.application;

public final class GameFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public enum Code { NOT_FOUND, FORBIDDEN, WRONG_TURN, VERSION_CONFLICT, COMMAND_CONFLICT, FINISHED, INVALID_MOVE }
    private final Code code;
    public GameFailure(Code code, String message) { super(message); this.code = code; }
    public Code code() { return code; }
}
