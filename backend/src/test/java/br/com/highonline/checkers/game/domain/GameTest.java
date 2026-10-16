package br.com.highonline.checkers.game.domain;

import br.com.highonline.checkers.player.domain.PlayerId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    @Test
    void acceptedMoveCreatesSnapshotAndAdvancesVersionOnce() {
        PlayerId black = PlayerId.newId(), white = PlayerId.newId();
        Game original = Game.start(black, white);
        Game next = original.play(white, new Move(new Square(5, 0), java.util.List.of(new Square(4, 1))), 0);
        assertEquals(original.id(), next.id());
        assertEquals(Color.BLACK, next.turn());
        assertEquals(1, next.version());
        assertEquals(0, original.version());
        assertNotNull(original.board().pieceAt(new Square(5, 0)));
        assertNull(next.board().pieceAt(new Square(5, 0)));
        assertEquals(GameStatus.IN_PROGRESS, next.status());
        assertTrue(next.winner().isEmpty());
    }

    @Test
    void rejectsWrongPlayerStaleVersionAndIllegalMoveWithoutChangingState() {
        PlayerId black = PlayerId.newId(), white = PlayerId.newId();
        Game game = Game.start(black, white);
        Move valid = game.legalMoves().get(0);
        assertThrows(IllegalArgumentException.class, () -> game.play(black, valid, 0));
        assertThrows(IllegalArgumentException.class, () -> game.play(PlayerId.newId(), valid, 0));
        assertThrows(StaleGameVersionException.class, () -> game.play(white, valid, 1));
        assertThrows(IllegalArgumentException.class, () -> game.play(white,
                new Move(new Square(5, 0), java.util.List.of(new Square(2, 3))), 0));
        assertEquals(0, game.version());
        assertEquals(Color.WHITE, game.turn());
        assertEquals(12, game.board().count(Color.BLACK));
    }

    @Test
    void playsCompleteGamesAndRejectsMovesAfterVictory() {
        // Fixed seeds exercise turns, forced captures and terminal detection from a real initial board.
        int completed = 0;
        for (int seed = 0; seed < 20; seed++) {
            PlayerId black = PlayerId.newId(), white = PlayerId.newId();
            Game game = Game.start(black, white);
            java.util.Random random = new java.util.Random(seed);
            for (int ply = 0; ply < 400 && game.status() == GameStatus.IN_PROGRESS; ply++) {
                var moves = game.legalMoves();
                PlayerId player = game.turn() == Color.WHITE ? white : black;
                game = game.play(player, moves.get(random.nextInt(moves.size())), game.version());
            }
            if (game.status() == GameStatus.FINISHED) {
                completed++;
                assertEquals(game.turn().opposite(), game.winner().orElseThrow());
                assertTrue(game.legalMoves().isEmpty());
                Game finished = game;
                assertThrows(IllegalStateException.class, () -> finished.play(white, null, finished.version()));
            }
        }
        assertTrue(completed > 0);
    }
    @Test
    void startsWithTwelvePiecesPerPlayerAndWhiteToMove() {
        PlayerId black = PlayerId.newId(), white = PlayerId.newId();
        Game game = Game.start(black, white);

        assertNotNull(game.id());
        assertEquals(black, game.blackPlayerId());
        assertEquals(white, game.whitePlayerId());
        assertEquals(Color.WHITE, game.turn());
        assertEquals(0, game.version());
        assertEquals(12, game.board().count(Color.BLACK));
        assertEquals(12, game.board().count(Color.WHITE));
        assertNull(game.board().pieceAt(new Square(3, 0)));
        assertEquals(new Piece(Color.BLACK, false), game.board().pieceAt(new Square(0, 1)));
    }

    @Test
    void rejectsTheSamePlayerOnBothSides() {
        PlayerId player = PlayerId.newId();
        assertThrows(IllegalArgumentException.class, () -> Game.start(player, player));
        assertThrows(IllegalArgumentException.class, () -> Game.start(null, player));
    }

    @Test
    void rejectsSquaresOutsideTheBoard() {
        assertThrows(IllegalArgumentException.class, () -> new Square(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Square(0, 8));
    }
}

