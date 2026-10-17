package br.com.highonline.checkers.game.infrastructure.postgres;

import br.com.highonline.checkers.game.application.PlayMove;
import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

/** Explicit versioned storage format, independent from the public HTTP contract. */
public final class GameJsonCodec {
    private final JsonMapper mapper = JsonMapper.builder().build();

    public String encode(Game game) {
        List<PieceState> pieces = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = game.board().pieceAt(new Square(row, column));
                if (piece != null) pieces.add(new PieceState(row, column, piece.color(), piece.king()));
            }
        }
        return mapper.writeValueAsString(new Snapshot(1, game.id().value(), game.blackPlayerId().value(),
                game.whitePlayerId().value(), game.turn(), game.version(), game.status(),
                game.winner().orElse(null), pieces));
    }

    public Game decode(String json) {
        Snapshot state = mapper.readValue(json, Snapshot.class);
        if (state.schemaVersion() != 1) throw new IllegalStateException("Unsupported game snapshot schema");
        Map<Square, Piece> pieces = new HashMap<>();
        for (PieceState piece : state.pieces()) {
            if (pieces.put(new Square(piece.row(), piece.column()), new Piece(piece.color(), piece.king())) != null) {
                throw new IllegalStateException("Duplicate piece position in snapshot");
            }
        }
        return Game.restore(new GameId(state.id()), new PlayerId(state.blackPlayerId()),
                new PlayerId(state.whitePlayerId()), Board.restore(pieces), state.turn(), state.version(),
                state.status(), state.winner());
    }

    public String encodeCommand(PlayMove command) { return mapper.writeValueAsString(command); }
    public PlayMove decodeCommand(String json) { return mapper.readValue(json, PlayMove.class); }

    public record PieceState(int row, int column, Color color, boolean king) {}
    public record Snapshot(int schemaVersion, UUID id, UUID blackPlayerId, UUID whitePlayerId,
                           Color turn, long version, GameStatus status, Color winner, List<PieceState> pieces) {}
}
