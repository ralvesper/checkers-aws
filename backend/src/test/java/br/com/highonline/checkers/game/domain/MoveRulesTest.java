package br.com.highonline.checkers.game.domain;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MoveRulesTest {
    private static Square s(int row, int column) { return new Square(row, column); }
    private static Piece white() { return new Piece(Color.WHITE, false); }
    private static Piece black() { return new Piece(Color.BLACK, false); }
    private static Move move(int row, int column, Square... landings) {
        return new Move(s(row, column), List.of(landings));
    }

    @Test
    void initialPositionHasSevenForwardMovesPerSide() {
        assertEquals(7, MoveRules.legalMoves(Board.initial(), Color.WHITE).size());
        assertEquals(7, MoveRules.legalMoves(Board.initial(), Color.BLACK).size());
        assertThrows(IllegalArgumentException.class, () -> MoveRules.apply(Board.initial(), Color.WHITE,
                move(5, 0, s(6, 1))));
    }

    @Test
    void backwardCaptureIsMandatoryAndRemovesVictim() {
        Board board = Board.of(Map.of(s(3, 2), white(), s(4, 3), black()));
        Move capture = move(3, 2, s(5, 4));
        assertEquals(List.of(capture), MoveRules.legalMoves(board, Color.WHITE));
        assertThrows(IllegalArgumentException.class, () -> MoveRules.apply(board, Color.WHITE,
                move(3, 2, s(2, 1))));
        Board result = MoveRules.apply(board, Color.WHITE, capture);
        assertNull(result.pieceAt(s(4, 3)));
        assertEquals(white(), result.pieceAt(s(5, 4)));
        assertEquals(1, board.count(Color.BLACK));
    }

    @Test
    void majorityRuleIsGlobalAndRejectsPartialSequences() {
        Board board = Board.of(Map.of(s(5, 0), white(), s(4, 1), black(), s(2, 3), black(),
                s(7, 6), white(), s(6, 5), black()));
        Move full = move(5, 0, s(3, 2), s(1, 4));
        assertEquals(List.of(full), MoveRules.legalMoves(board, Color.WHITE));
        assertThrows(IllegalArgumentException.class, () -> MoveRules.apply(board, Color.WHITE,
                move(5, 0, s(3, 2))));
        assertThrows(IllegalArgumentException.class, () -> MoveRules.apply(board, Color.WHITE,
                move(7, 6, s(5, 4))));
        assertEquals(1, MoveRules.apply(board, Color.WHITE, full).count(Color.BLACK));
    }

    @Test
    void kingSlidesAndMayChooseLandingBeyondVictim() {
        Piece king = new Piece(Color.WHITE, true);
        Board emptyDiagonal = Board.of(Map.of(s(5, 0), king));
        assertTrue(MoveRules.legalMoves(emptyDiagonal, Color.WHITE).contains(move(5, 0, s(0, 5))));
        Board board = Board.of(Map.of(s(5, 0), king, s(3, 2), black()));
        List<Move> moves = MoveRules.legalMoves(board, Color.WHITE);
        assertEquals(3, moves.size());
        assertTrue(moves.contains(move(5, 0, s(2, 3))));
        assertTrue(moves.contains(move(5, 0, s(0, 5))));
    }

    @Test
    void capturedPieceBlocksReverseDiagonalUntilTurnEnds() {
        Board board = Board.of(Map.of(s(4, 3), new Piece(Color.WHITE, true),
                s(3, 4), black(), s(5, 2), black()));
        // Removing the first victim too early would allow a second capture on the same diagonal.
        for (Move move : MoveRules.legalMoves(board, Color.WHITE)) {
            assertEquals(1, move.landings().size());
            assertEquals(1, MoveRules.apply(board, Color.WHITE, move).count(Color.BLACK));
        }
        assertThrows(IllegalArgumentException.class, () -> MoveRules.apply(board, Color.WHITE,
                move(4, 3, s(2, 5), s(6, 1))));
    }

    @Test
    void crownsOnlyAtEndOfTurn() {
        Board board = Board.of(Map.of(s(2, 1), white(), s(1, 2), black(), s(1, 4), black()));
        Move chain = move(2, 1, s(0, 3), s(2, 5));
        assertEquals(List.of(chain), MoveRules.legalMoves(board, Color.WHITE));
        assertFalse(MoveRules.apply(board, Color.WHITE, chain).pieceAt(s(2, 5)).king());
        Board promotion = Board.of(Map.of(s(1, 2), white()));
        assertTrue(MoveRules.apply(promotion, Color.WHITE, move(1, 2, s(0, 1))).pieceAt(s(0, 1)).king());
        Board blackPromotion = Board.of(Map.of(s(6, 1), black()));
        assertTrue(MoveRules.apply(blackPromotion, Color.BLACK, move(6, 1, s(7, 2))).pieceAt(s(7, 2)).king());
    }

    @Test
    void sameVictimCannotBeCapturedAgain() {
        Board board = Board.of(Map.of(s(5, 0), new Piece(Color.WHITE, true), s(3, 2), black()));
        assertThrows(IllegalArgumentException.class, () -> MoveRules.apply(board, Color.WHITE,
                move(5, 0, s(2, 3), s(4, 1))));
    }

    @Test
    void equalCaptureCountsAllowEitherPathRegardlessOfVictimRank() {
        Board board = Board.of(Map.of(s(5, 2), white(), s(4, 1), black(),
                s(4, 3), new Piece(Color.BLACK, true)));
        var moves = MoveRules.legalMoves(board, Color.WHITE);
        assertEquals(2, moves.size());
        assertTrue(moves.contains(move(5, 2, s(3, 0))));
        assertTrue(moves.contains(move(5, 2, s(3, 4))));
    }

    @Test
    void kingCannotJumpOwnPieceOrTwoOpponentsInOneJump() {
        Board ownBlocker = Board.of(Map.of(s(5, 0), new Piece(Color.WHITE, true), s(4, 1), white()));
        assertThrows(IllegalArgumentException.class, () -> MoveRules.apply(ownBlocker, Color.WHITE,
                move(5, 0, s(3, 2))));
        Board opponents = Board.of(Map.of(s(5, 0), new Piece(Color.WHITE, true),
                s(4, 1), black(), s(3, 2), black()));
        assertThrows(IllegalArgumentException.class, () -> MoveRules.apply(opponents, Color.WHITE,
                move(5, 0, s(2, 3))));
    }

    @Test
    void blockedSideHasNoLegalMoves() {
        Board board = Board.of(Map.of(s(7, 0), black(), s(0, 1), white()));
        assertTrue(MoveRules.legalMoves(board, Color.BLACK).isEmpty());
        assertTrue(MoveRules.legalMoves(Board.of(Map.of(s(4, 1), white())), Color.BLACK).isEmpty());
    }

    @Test
    void rejectsInvalidPathsAndDefensivelyCopiesLandings() {
        assertThrows(IllegalArgumentException.class, () -> new Move(s(5, 0), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Move(s(5, 0), List.of(s(4, 0))));
        List<Square> landings = new ArrayList<>(List.of(s(4, 1)));
        Move move = new Move(s(5, 0), landings);
        landings.clear();
        assertEquals(1, move.landings().size());
        assertThrows(UnsupportedOperationException.class, () -> move.landings().clear());
    }
}
