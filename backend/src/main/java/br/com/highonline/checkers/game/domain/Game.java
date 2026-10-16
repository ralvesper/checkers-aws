package br.com.highonline.checkers.game.domain;

import br.com.highonline.checkers.player.domain.PlayerId;
import java.util.List;
import java.util.Optional;

/** Immutable aggregate: an accepted turn produces a new game snapshot. */
public final class Game {
    private final GameId id;
    private final PlayerId blackPlayerId;
    private final PlayerId whitePlayerId;
    private final Board board;
    private final Color turn;
    private final long version;
    private final GameStatus status;
    private final Color winner;

    private Game(GameId id, PlayerId blackPlayerId, PlayerId whitePlayerId, Board board, Color turn, long version) {
        this(id, blackPlayerId, whitePlayerId, board, turn, version, GameStatus.IN_PROGRESS, null);
    }

    private Game(GameId id, PlayerId blackPlayerId, PlayerId whitePlayerId, Board board,
                 Color turn, long version, GameStatus status, Color winner) {
        this.id = id;
        this.blackPlayerId = blackPlayerId;
        this.whitePlayerId = whitePlayerId;
        this.board = board;
        this.turn = turn;
        this.version = version;
        this.status = status;
        this.winner = winner;
    }

    public static Game start(PlayerId blackPlayerId, PlayerId whitePlayerId) {
        if (blackPlayerId == null || whitePlayerId == null || blackPlayerId.equals(whitePlayerId)) {
            throw new IllegalArgumentException("A game requires two different players");
        }
        return new Game(GameId.newId(), blackPlayerId, whitePlayerId, Board.initial(), Color.WHITE, 0);
    }

    public GameId id() { return id; }

    public List<Move> legalMoves() {
        return status == GameStatus.FINISHED ? List.of() : MoveRules.legalMoves(board, turn);
    }

    public Game play(PlayerId playerId, Move move, long expectedVersion) {
        if (expectedVersion != version) throw new StaleGameVersionException();
        if (status == GameStatus.FINISHED) throw new IllegalStateException("Game has finished");
        PlayerId currentPlayer = turn == Color.WHITE ? whitePlayerId : blackPlayerId;
        if (!currentPlayer.equals(playerId)) throw new IllegalArgumentException("Not the current player");
        if (move == null) throw new IllegalArgumentException("Move is required");
        Board nextBoard = MoveRules.apply(board, turn, move);
        Color nextTurn = turn.opposite();
        boolean finished = MoveRules.legalMoves(nextBoard, nextTurn).isEmpty();
        return new Game(id, blackPlayerId, whitePlayerId, nextBoard, nextTurn, version + 1,
                finished ? GameStatus.FINISHED : GameStatus.IN_PROGRESS, finished ? turn : null);
    }

    public GameStatus status() { return status; }
    public Optional<Color> winner() { return Optional.ofNullable(winner); }
    public PlayerId blackPlayerId() { return blackPlayerId; }
    public PlayerId whitePlayerId() { return whitePlayerId; }
    public Board board() { return board; }
    public Color turn() { return turn; }
    public long version() { return version; }
}

