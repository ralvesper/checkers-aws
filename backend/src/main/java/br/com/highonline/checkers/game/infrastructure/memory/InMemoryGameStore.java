package br.com.highonline.checkers.game.infrastructure.memory;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Single-process local adapter. State and receipts are lost on restart. */
public final class InMemoryGameStore implements GameStore {
    private final ConcurrentHashMap<GameId, StoredGame> games = new ConcurrentHashMap<>();

    @Override
    public void create(Game game) {
        if (games.putIfAbsent(game.id(), new StoredGame(game, Map.of())) != null) {
            throw new IllegalStateException("Game already exists");
        }
    }

    @Override
    public Optional<Game> find(GameId id) {
        return Optional.ofNullable(games.get(id)).map(StoredGame::game);
    }

    @Override
    public Game update(GameId id, Function<StoredGame, Change> operation) {
        AtomicReference<Game> response = new AtomicReference<>();
        games.compute(id, (key, current) -> {
            if (current == null) throw new GameFailure(GameFailure.Code.NOT_FOUND, "Game not found");
            Change change = operation.apply(current);
            response.set(change.response());
            return change.stored();
        });
        return response.get();
    }
}
