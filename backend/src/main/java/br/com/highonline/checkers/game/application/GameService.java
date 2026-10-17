package br.com.highonline.checkers.game.application;

import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import java.util.HashMap;
import static br.com.highonline.checkers.game.application.GameFailure.Code.*;

public final class GameService {
    private final GameStore store;
    public GameService(GameStore store) { this.store = store; }

    public Game create(PlayerId actor, PlayerId black, PlayerId white) {
        Game game = Game.start(black, white);
        requireParticipant(game, actor);
        store.create(game);
        return game;
    }

    public Game get(GameId id, PlayerId actor) {
        Game game = store.find(id).orElseThrow(() -> new GameFailure(NOT_FOUND, "Game not found"));
        requireParticipant(game, actor);
        return game;
    }

    public Game play(GameId id, PlayMove command) {
        if (command == null) throw new IllegalArgumentException("Command is required");
        return store.update(id, stored -> {
            Game game = stored.game();
            requireParticipant(game, command.playerId());
            var previous = stored.receipts().get(command.clientMoveId());
            if (previous != null) {
                if (!previous.command().equals(command)) {
                    throw new GameFailure(COMMAND_CONFLICT, "clientMoveId already used with another command");
                }
                return new GameStore.Change(stored, previous.result());
            }
            if (game.version() != command.expectedVersion()) throw new GameFailure(VERSION_CONFLICT, "Stale game version");
            if (game.status() == GameStatus.FINISHED) throw new GameFailure(FINISHED, "Game has finished");
            PlayerId current = game.turn() == Color.WHITE ? game.whitePlayerId() : game.blackPlayerId();
            if (!current.equals(command.playerId())) throw new GameFailure(WRONG_TURN, "Not your turn");
            Game next;
            try {
                next = game.play(command.playerId(), command.move(), command.expectedVersion());
            } catch (IllegalArgumentException invalid) {
                throw new GameFailure(INVALID_MOVE, invalid.getMessage());
            }
            var receipts = new HashMap<>(stored.receipts());
            receipts.put(command.clientMoveId(), new GameStore.Receipt(command, next));
            return new GameStore.Change(new GameStore.StoredGame(next, receipts), next);
        });
    }

    private static void requireParticipant(Game game, PlayerId actor) {
        if (!game.blackPlayerId().equals(actor) && !game.whitePlayerId().equals(actor)) {
            throw new GameFailure(FORBIDDEN, "Player is not a participant");
        }
    }
}
