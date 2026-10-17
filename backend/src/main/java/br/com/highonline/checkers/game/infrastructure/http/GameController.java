package br.com.highonline.checkers.game.infrastructure.http;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import br.com.highonline.checkers.game.infrastructure.realtime.GameStreamHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Local prototype identity only. Replace header identity with authenticated principal before hosting. */
@RestController
@RequestMapping("/games")
@Profile({"local", "local-postgres"})
public class GameController {
    private final GameService service;
    private final GameStreamHandler stream;
    public GameController(GameService service, GameStreamHandler stream) { this.service = service; this.stream = stream; }

    @PostMapping
    public ResponseEntity<GameResponse> create(@RequestHeader("X-Player-Id") UUID actor,
                                                @RequestBody CreateGameRequest request) {
        Game game = service.create(new PlayerId(actor), new PlayerId(request.blackPlayerId()),
                new PlayerId(request.whitePlayerId()));
        return ResponseEntity.created(URI.create("/games/" + game.id().value())).body(GameResponse.from(game));
    }

    @GetMapping("/{id}")
    public GameResponse get(@PathVariable("id") UUID id, @RequestHeader("X-Player-Id") UUID actor) {
        return GameResponse.from(service.get(new GameId(id), new PlayerId(actor)));
    }

    @PostMapping("/{id}/moves")
    public GameResponse play(@PathVariable("id") UUID id, @RequestHeader("X-Player-Id") UUID actor,
                              @RequestBody MoveRequest request) {
        PlayMove command = new PlayMove(new PlayerId(actor),
                new Move(request.from().toSquare(), request.landings().stream().map(Position::toSquare).toList()),
                request.expectedVersion(), request.clientMoveId());
        Game confirmed = service.play(new GameId(id), command);
        stream.publish(confirmed);
        return GameResponse.from(confirmed);
    }

    public record CreateGameRequest(UUID blackPlayerId, UUID whitePlayerId) {
        public CreateGameRequest {
            if (blackPlayerId == null || whitePlayerId == null) throw new IllegalArgumentException("Both players are required");
        }
    }

    public record Position(Integer row, Integer column) {
        public Position {
            if (row == null || column == null) throw new IllegalArgumentException("Row and column are required");
            new Square(row, column);
        }
        Square toSquare() { return new Square(row, column); }
        static Position from(Square square) { return new Position(square.row(), square.column()); }
    }

    public record MoveRequest(Position from, List<Position> landings, Long expectedVersion, UUID clientMoveId) {
        public MoveRequest {
            if (from == null || landings == null || landings.isEmpty() || landings.size() > 12
                    || landings.stream().anyMatch(position -> position == null)
                    || expectedVersion == null || expectedVersion < 0 || clientMoveId == null) {
                throw new IllegalArgumentException("Origin, 1 to 12 landings, version and clientMoveId are required");
            }
            landings = List.copyOf(landings);
        }
    }

    public record PieceResponse(int row, int column, Color color, boolean king) {}
    public record MoveResponse(Position from, List<Position> landings) {
        static MoveResponse from(Move move) {
            return new MoveResponse(Position.from(move.from()), move.landings().stream().map(Position::from).toList());
        }
    }
    public record GameResponse(UUID id, UUID blackPlayerId, UUID whitePlayerId, Color turn,
                               UUID currentPlayerId, long version, GameStatus status, Color winner,
                               List<PieceResponse> pieces, List<MoveResponse> legalMoves) {
        public static GameResponse from(Game game) {
            List<PieceResponse> pieces = new ArrayList<>();
            for (int row = 0; row < 8; row++) {
                for (int column = 0; column < 8; column++) {
                    Piece piece = game.board().pieceAt(new Square(row, column));
                    if (piece != null) pieces.add(new PieceResponse(row, column, piece.color(), piece.king()));
                }
            }
            UUID current = game.status() == GameStatus.FINISHED ? null
                    : (game.turn() == Color.WHITE ? game.whitePlayerId() : game.blackPlayerId()).value();
            return new GameResponse(game.id().value(), game.blackPlayerId().value(), game.whitePlayerId().value(),
                    game.turn(), current, game.version(), game.status(), game.winner().orElse(null),
                    List.copyOf(pieces), game.legalMoves().stream().map(MoveResponse::from).toList());
        }
    }
}
