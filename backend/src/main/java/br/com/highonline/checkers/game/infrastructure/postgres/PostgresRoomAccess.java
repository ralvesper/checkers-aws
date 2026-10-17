package br.com.highonline.checkers.game.infrastructure.postgres;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import static br.com.highonline.checkers.game.application.GameFailure.Code.*;

public final class PostgresRoomAccess implements RoomAccessStore {
    private final GameStore games;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public PostgresRoomAccess(GameStore games, JdbcTemplate jdbc, TransactionTemplate tx) {
        this.games = games; this.jdbc = jdbc; this.tx = tx;
    }
    public void create(Game game, UUID owner, String hash, Instant expiry) {
        tx.executeWithoutResult(status -> {
            games.create(game);
            jdbc.update("INSERT INTO game_access(game_id, player_id, side, session_id) VALUES (?, ?, 'WHITE', ?)",
                    game.id().value(), game.whitePlayerId().value(), owner);
            jdbc.update("INSERT INTO game_access(game_id, player_id, side, invitation_hash, invitation_expires_at) VALUES (?, ?, 'BLACK', ?, ?)",
                    game.id().value(), game.blackPlayerId().value(), hash, Timestamp.from(expiry));
        });
    }
    public PlayerId seat(GameId id, UUID session) {
        return jdbc.query("SELECT player_id FROM game_access WHERE game_id = ? AND session_id = ?",
                (rs, row) -> new PlayerId(rs.getObject(1, UUID.class)), id.value(), session).stream().findFirst()
                .orElseThrow(() -> new GameFailure(FORBIDDEN, "No seat for this session"));
    }
    public PlayerId join(GameId id, UUID session, String hash, Instant now) {
        return tx.execute(status -> {
            var owner = jdbc.query("SELECT session_id FROM game_access WHERE game_id = ? AND side = 'WHITE'",
                    (rs, row) -> rs.getObject(1, UUID.class), id.value());
            if (owner.contains(session)) throw new GameFailure(FORBIDDEN, "Owner already has a seat");
            // Lock one invitation row: only one guest can claim it, even across API instances.
            var rows = jdbc.query("SELECT player_id, session_id, invitation_hash, invitation_expires_at FROM game_access WHERE game_id = ? AND side = 'BLACK' FOR UPDATE",
                    (rs, row) -> new Invite(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3),
                            rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant()), id.value());
            if (rows.isEmpty()) throw new GameFailure(FORBIDDEN, "Invitation unavailable");
            Invite invite = rows.get(0);
            if (session.equals(invite.session())) return new PlayerId(invite.player());
            if (invite.session() != null || !hash.equals(invite.hash()) || invite.expiry() == null || !now.isBefore(invite.expiry())) {
                throw new GameFailure(INVITATION_UNAVAILABLE, "Invitation expired, invalid or already claimed");
            }
            jdbc.update("UPDATE game_access SET session_id = ?, invitation_hash = NULL, invitation_expires_at = NULL WHERE game_id = ? AND side = 'BLACK'",
                    session, id.value());
            return new PlayerId(invite.player());
        });
    }
    private record Invite(UUID player, UUID session, String hash, Instant expiry) {}
}
