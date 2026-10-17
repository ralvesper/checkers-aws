package br.com.highonline.checkers.game.infrastructure.http;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import br.com.highonline.checkers.game.infrastructure.realtime.GameStreamHandler;
import br.com.highonline.checkers.game.infrastructure.security.GuestTokens;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Guest session capabilities bind server-issued credentials to private seat ownership. */
@RestController
@RequestMapping("/games")
@Profile({"local", "local-postgres"})
public class GameController {
    @ModelAttribute
    public void noCache(jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control", "no-store"); }
    private final GameService service;
    private final GameStreamHandler stream;
    private final RoomService rooms;
    private final GuestTokens tokens;
    public GameController(GameService service, GameStreamHandler stream, RoomService rooms, GuestTokens tokens) {
        this.service = service; this.stream = stream; this.rooms = rooms; this.tokens = tokens;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@RequestHeader(value = "Authorization", required = false) String auth) {
        RoomService.Created room = rooms.create(tokens.authorization(auth));
        return ResponseEntity.created(URI.create("/games/" + room.game().id().value()))
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new RoomResponse(GameResponse.from(room.game()), room.invitation()));
    }

    public record RoomResponse(GameResponse game, String invitation) {}
    public record JoinRequest(String invitation) {}
    @PostMapping("/{id}/join")
    public GameResponse join(@PathVariable("id") UUID id,
                             @RequestHeader(value = "Authorization", required = false) String auth,
                             @RequestBody JoinRequest request) {
        var seat = rooms.join(new GameId(id), tokens.authorization(auth), request.invitation());
        return GameResponse.from(service.get(new GameId(id), seat));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameResponse> get(@PathVariable("id") UUID id, @RequestHeader(value = "Authorization", required = false) String auth) {
        var seat = rooms.seat(new GameId(id), tokens.authorization(auth));
        return ResponseEntity.ok().header("X-Seat-Id", seat.value().toString()).body(GameResponse.from(service.get(new GameId(id), seat)));
    }

    @PostMapping("/{id}/moves")
    public GameResponse play(@PathVariable("id") UUID id, @RequestHeader(value = "Authorization", required = false) String auth,
                              @RequestBody MoveRequest request) {
        PlayMove command = new PlayMove(rooms.seat(new GameId(id), tokens.authorization(auth)),
                new Move(request.from().toSquare(), request.landings().stream().map(Position::toSquare).toList()),
                request.expectedVersion(), request.clientMoveId());
        Game confirmed = service.play(new GameId(id), command);
        stream.publish(confirmed);
        return GameResponse.from(confirmed);
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
