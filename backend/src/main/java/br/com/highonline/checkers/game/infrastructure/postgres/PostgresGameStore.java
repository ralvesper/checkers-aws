package br.com.highonline.checkers.game.infrastructure.postgres;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.function.Function;
import static br.com.highonline.checkers.game.application.GameFailure.Code.*;

public final class PostgresGameStore implements GameStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final GameJsonCodec codec = new GameJsonCodec();

    public PostgresGameStore(JdbcTemplate jdbc, TransactionTemplate transaction) {
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    @Override
    public void create(Game game) {
        jdbc.update("INSERT INTO games(id, version, state) VALUES (?, ?, ?::jsonb)",
                game.id().value(), game.version(), codec.encode(game));
    }

    @Override
    public Optional<Game> find(GameId id) {
        return jdbc.query("SELECT state::text FROM games WHERE id = ?",
                (rs, row) -> codec.decode(rs.getString(1)), id.value()).stream().findFirst();
    }

    @Override
    public Game update(GameId id, Function<StoredGame, Change> operation) {
        // A losing CAS is rolled back before retrying against a fresh committed snapshot.
        // The application then either returns the original receipt or reports a stale version.
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return transaction.execute(status -> apply(id, operation));
            } catch (RetryVersionConflict collision) {
                if (attempt == 1) throw new GameFailure(VERSION_CONFLICT, "Concurrent game update");
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private Game apply(GameId id, Function<StoredGame, Change> operation) {
        StoredGame current = load(id);
        Change change = operation.apply(current);
        if (change.stored() == current) return change.response(); // Idempotent replay: no writes.
        Game next = change.stored().game();
        int updated = jdbc.update("""
                UPDATE games SET version = ?, state = ?::jsonb, updated_at = now()
                WHERE id = ? AND version = ?
                """, next.version(), codec.encode(next), id.value(), current.game().version());
        if (updated == 0) throw new RetryVersionConflict();
        for (var entry : change.stored().receipts().entrySet()) {
            if (current.receipts().containsKey(entry.getKey())) continue;
            Receipt receipt = entry.getValue();
            jdbc.update("""
                    INSERT INTO game_moves(game_id, client_move_id, resulting_version, command, result)
                    VALUES (?, ?, ?, ?::jsonb, ?::jsonb)
                    """, id.value(), entry.getKey(), receipt.result().version(),
                    codec.encodeCommand(receipt.command()), codec.encode(receipt.result()));
        }
        return change.response();
    }

    private StoredGame load(GameId id) {
        // One statement gives a consistent state+receipt snapshot at READ COMMITTED.
        return jdbc.query("""
                SELECT g.state::text AS state, m.client_move_id, m.command::text AS command,
                       m.result::text AS result
                FROM games g LEFT JOIN game_moves m ON m.game_id = g.id WHERE g.id = ?
                """, rs -> {
            Game game = null;
            Map<UUID, Receipt> receipts = new HashMap<>();
            while (rs.next()) {
                if (game == null) game = codec.decode(rs.getString("state"));
                UUID commandId = rs.getObject("client_move_id", UUID.class);
                if (commandId != null) receipts.put(commandId,
                        new Receipt(codec.decodeCommand(rs.getString("command")), codec.decode(rs.getString("result"))));
            }
            if (game == null) throw new GameFailure(NOT_FOUND, "Game not found");
            return new StoredGame(game, receipts);
        }, id.value());
    }

    private static final class RetryVersionConflict extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
