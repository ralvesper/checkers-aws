package br.com.highonline.checkers.game.infrastructure.postgres;

import br.com.highonline.checkers.game.application.PlayMove;
import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GameJsonCodecTest {
    private final GameJsonCodec codec = new GameJsonCodec();

    @Test
    void roundTripsInitialAndPlayedSnapshotAndCommand() {
        PlayerId black = PlayerId.newId(), white = PlayerId.newId();
        Game game = Game.start(black, white);
        assertEquals(codec.encode(game), codec.encode(codec.decode(codec.encode(game))));
        PlayMove command = new PlayMove(white, game.legalMoves().get(0), 0, UUID.randomUUID());
        assertEquals(command, codec.decodeCommand(codec.encodeCommand(command)));
        Game next = game.play(white, command.move(), 0);
        Game restored = codec.decode(codec.encode(next));
        assertEquals(codec.encode(next), codec.encode(restored));
        assertEquals(Set.copyOf(next.legalMoves()), Set.copyOf(restored.legalMoves()));
    }

    @Test
    void roundTripsKingAndFinishedGame() {
        Game finished = Game.restore(GameId.newId(), PlayerId.newId(), PlayerId.newId(),
                Board.restore(Map.of(new Square(0, 1), new Piece(Color.WHITE, true))),
                Color.BLACK, 9, GameStatus.FINISHED, Color.WHITE);
        Game restored = codec.decode(codec.encode(finished));
        assertEquals(GameStatus.FINISHED, restored.status());
        assertEquals(Optional.of(Color.WHITE), restored.winner());
        assertTrue(restored.board().pieceAt(new Square(0, 1)).king());
        assertTrue(restored.legalMoves().isEmpty());
    }

    @Test
    void rejectsUnknownSchemaAndInconsistentSnapshot() {
        Game game = Game.start(PlayerId.newId(), PlayerId.newId());
        String json = codec.encode(game);
        assertThrows(IllegalStateException.class, () -> codec.decode(json.replace("\"schemaVersion\":1", "\"schemaVersion\":2")));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(json.replace("\"version\":0", "\"version\":-1")));
        assertThrows(IllegalArgumentException.class, () -> Game.restore(game.id(), game.blackPlayerId(),
                game.whitePlayerId(), game.board(), game.turn(), 0, GameStatus.FINISHED, Color.BLACK));
    }
}
