package br.com.highonline.checkers.game.infrastructure.memory;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static br.com.highonline.checkers.game.application.GameFailure.Code.*;

public final class InMemoryRoomAccess implements RoomAccessStore {
    private final GameStore games;
    private final ConcurrentHashMap<GameId, Access> rooms = new ConcurrentHashMap<>();
    public InMemoryRoomAccess(GameStore games) { this.games = games; }
    public void create(Game game, UUID owner, String hash, Instant expiry) {
        games.create(game);
        rooms.put(game.id(), new Access(game, owner, null, hash, expiry));
    }
    public PlayerId seat(GameId id, UUID session) {
        Access access = rooms.get(id);
        if (access == null) throw new GameFailure(FORBIDDEN, "No seat for this session");
        if (access.owner().equals(session)) return access.game().whitePlayerId();
        if (session.equals(access.guest())) return access.game().blackPlayerId();
        throw new GameFailure(FORBIDDEN, "No seat for this session");
    }
    public PlayerId join(GameId id, UUID session, String hash, Instant now) {
        rooms.compute(id, (key, access) -> {
            if (access == null) throw new GameFailure(FORBIDDEN, "Invitation unavailable");
            if (access.owner().equals(session)) throw new GameFailure(FORBIDDEN, "Owner already has a seat");
            if (session.equals(access.guest())) return access;
            if (access.guest() != null || !access.hash().equals(hash) || !now.isBefore(access.expiry())) {
                throw new GameFailure(INVITATION_UNAVAILABLE, "Invitation expired, invalid or already claimed");
            }
            return new Access(access.game(), access.owner(), session, null, access.expiry());
        });
        return seat(id, session);
    }
    private record Access(Game game, UUID owner, UUID guest, String hash, Instant expiry) {}
}
