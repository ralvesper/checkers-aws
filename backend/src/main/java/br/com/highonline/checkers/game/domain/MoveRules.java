package br.com.highonline.checkers.game.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Brazilian 8x8 rules. Captured pieces block diagonals until the turn ends. */
final class MoveRules {
    private static final int[] DIRECTIONS = {-1, 1};

    private MoveRules() {}

    static List<Move> legalMoves(Board board, Color color) {
        return candidates(board, color).stream().map(Candidate::move).toList();
    }

    static Board apply(Board board, Color color, Move move) {
        Candidate selected = candidates(board, color).stream()
                .filter(candidate -> candidate.move().equals(move)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Illegal or incomplete move"));
        Map<Square, Piece> result = new HashMap<>(board.pieces());
        Piece piece = result.remove(move.from());
        selected.captured().forEach(result::remove);
        Square destination = move.landings().get(move.landings().size() - 1);
        boolean crowned = destination.row() == (color == Color.WHITE ? 0 : 7);
        result.put(destination, new Piece(color, piece.king() || crowned));
        return Board.of(result);
    }

    private static List<Candidate> candidates(Board board, Color color) {
        List<Candidate> captures = new ArrayList<>();
        board.pieces().forEach((origin, piece) -> {
            if (piece.color() == color) {
                Map<Square, Piece> position = new HashMap<>(board.pieces());
                position.remove(origin);
                capturePaths(position, origin, origin, piece, List.of(), Set.of(), captures);
            }
        });
        int maximum = captures.stream().mapToInt(candidate -> candidate.captured().size()).max().orElse(0);
        if (maximum > 0) {
            return captures.stream().filter(candidate -> candidate.captured().size() == maximum).toList();
        }
        List<Candidate> moves = new ArrayList<>();
        board.pieces().forEach((origin, piece) -> {
            if (piece.color() != color) return;
            for (int dr : DIRECTIONS) {
                if (!piece.king() && dr != (color == Color.WHITE ? -1 : 1)) continue;
                for (int dc : DIRECTIONS) {
                    for (int distance = 1; distance <= (piece.king() ? 7 : 1); distance++) {
                        Square landing = square(origin.row() + dr * distance, origin.column() + dc * distance);
                        if (landing == null || board.pieceAt(landing) != null) break;
                        moves.add(new Candidate(new Move(origin, List.of(landing)), Set.of()));
                    }
                }
            }
        });
        return List.copyOf(moves);
    }

    private static void capturePaths(Map<Square, Piece> position, Square origin, Square current,
                                     Piece piece, List<Square> path, Set<Square> captured,
                                     List<Candidate> results) {
        boolean extended = false;
        for (int dr : DIRECTIONS) {
            for (int dc : DIRECTIONS) {
                Square victim = null;
                for (int distance = 1; distance <= (piece.king() ? 7 : 2); distance++) {
                    Square next = square(current.row() + dr * distance, current.column() + dc * distance);
                    if (next == null) break;
                    Piece occupant = position.get(next);
                    if (occupant != null) {
                        if (victim != null || occupant.color() == piece.color() || captured.contains(next)) break;
                        victim = next;
                    } else if (victim != null) {
                        List<Square> nextPath = new ArrayList<>(path);
                        nextPath.add(next);
                        Set<Square> nextCaptured = new HashSet<>(captured);
                        nextCaptured.add(victim);
                        capturePaths(position, origin, next, piece, nextPath, nextCaptured, results);
                        extended = true;
                        if (!piece.king()) break;
                    } else if (!piece.king()) {
                        break;
                    }
                }
            }
        }
        if (!extended && !captured.isEmpty()) {
            results.add(new Candidate(new Move(origin, path), Set.copyOf(captured)));
        }
    }

    private static Square square(int row, int column) {
        return row < 0 || row > 7 || column < 0 || column > 7 ? null : new Square(row, column);
    }

    private record Candidate(Move move, Set<Square> captured) {}
}
