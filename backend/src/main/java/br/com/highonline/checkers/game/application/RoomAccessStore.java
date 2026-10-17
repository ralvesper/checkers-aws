package br.com.highonline.checkers.game.application;

import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import java.time.Instant;
import java.util.UUID;

/** Seat ownership is separate from public player IDs. Creating room and grants must be atomic. */
public interface RoomAccessStore {
    void create(Game game, UUID ownerSession, String invitationHash, Instant expiresAt);
    PlayerId seat(GameId id, UUID session);
    PlayerId join(GameId id, UUID session, String invitationHash, Instant now);
}
