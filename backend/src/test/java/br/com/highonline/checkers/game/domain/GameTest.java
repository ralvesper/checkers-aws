package br.com.highonline.checkers.game.domain;

import br.com.highonline.checkers.player.domain.PlayerId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    @Test
    void startsWithTwelvePiecesPerPlayerAndBlackToMove() {
        PlayerId black = PlayerId.newId(), white = PlayerId.newId();
        Game game = Game.start(black, white);

        assertNotNull(game.id());
        assertEquals(black, game.blackPlayerId());
        assertEquals(white, game.whitePlayerId());
        assertEquals(Color.BLACK, game.turn());
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
