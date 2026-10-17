package br.com.highonline.checkers.game.application;

import br.com.highonline.checkers.game.domain.Move;
import br.com.highonline.checkers.player.domain.PlayerId;
import java.util.UUID;

public record PlayMove(PlayerId playerId, Move move, long expectedVersion, UUID clientMoveId) {
    public PlayMove {
        if (playerId == null || move == null || expectedVersion < 0 || clientMoveId == null) {
            throw new IllegalArgumentException("Player, move, nonnegative version and clientMoveId are required");
        }
    }
}
