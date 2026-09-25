package br.com.highonline.checkers.game.domain;

import br.com.highonline.checkers.player.domain.PlayerId;

/** Aggregate root for a game. Moves and turn transitions are added with the movement rules. */
public final class Game {
    private final GameId id;
    private final PlayerId blackPlayerId;
    private final PlayerId whitePlayerId;
    private final Board board;
    private final Color turn;
    private final long version;

    private Game(GameId id, PlayerId blackPlayerId, PlayerId whitePlayerId, Board board, Color turn, long version) {
        this.id = id;
        this.blackPlayerId = blackPlayerId;
        this.whitePlayerId = whitePlayerId;
        this.board = board;
        this.turn = turn;
        this.version = version;
    }

    public static Game start(PlayerId blackPlayerId, PlayerId whitePlayerId) {
        if (blackPlayerId == null || whitePlayerId == null || blackPlayerId.equals(whitePlayerId)) {
            throw new IllegalArgumentException("A game requires two different players");
        }
        return new Game(GameId.newId(), blackPlayerId, whitePlayerId, Board.initial(), Color.WHITE, 0);
    }

    public GameId id() { return id; }
    public PlayerId blackPlayerId() { return blackPlayerId; }
    public PlayerId whitePlayerId() { return whitePlayerId; }
    public Board board() { return board; }
    public Color turn() { return turn; }
    public long version() { return version; }
}
