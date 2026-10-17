package br.com.highonline.checkers.game.application;

import br.com.highonline.checkers.game.domain.Game;
import br.com.highonline.checkers.game.domain.GameId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** A change must atomically commit both state and receipts, or commit neither.
 * Update callbacks may be retried after a version collision and must have no external side effects.
 */
public interface GameStore {
    void create(Game game);
    Optional<Game> find(GameId id);
    Game update(GameId id, Function<StoredGame, Change> operation);

    record Receipt(PlayMove command, Game result) {}
    record StoredGame(Game game, Map<UUID, Receipt> receipts) {
        public StoredGame { receipts = Map.copyOf(receipts); }
    }
    record Change(StoredGame stored, Game response) {}
}
