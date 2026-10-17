package br.com.highonline.checkers.game.application;

import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.game.infrastructure.memory.InMemoryGameStore;
import br.com.highonline.checkers.player.domain.PlayerId;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.highonline.checkers.game.application.GameFailure.Code.*;

class GameServiceTest {
    private final GameService service = new GameService(new InMemoryGameStore());
    private final PlayerId black = PlayerId.newId(), white = PlayerId.newId();
    private Game create() { return service.create(white, black, white); }
    private PlayMove command(PlayerId player, Move move, long version) {
        return new PlayMove(player, move, version, UUID.randomUUID());
    }
    private void fails(GameFailure.Code code, org.junit.jupiter.api.function.Executable action) {
        assertEquals(code, assertThrows(GameFailure.class, action).code());
    }

    @Test
    void createsAndReadsOnlyForParticipants() {
        Game game = create();
        assertEquals(game.id(), service.get(game.id(), black).id());
        fails(FORBIDDEN, () -> service.get(game.id(), PlayerId.newId()));
        fails(FORBIDDEN, () -> service.create(PlayerId.newId(), black, white));
        fails(NOT_FOUND, () -> service.get(GameId.newId(), white));
        assertThrows(IllegalArgumentException.class, () -> service.create(white, white, white));
    }

    @Test
    void replayReturnsOriginalResultEvenAfterNextTurn() {
        Game game = create();
        PlayMove first = command(white, game.legalMoves().get(0), 0);
        Game one = service.play(game.id(), first);
        Game two = service.play(game.id(), command(black, one.legalMoves().get(0), 1));
        assertEquals(2, two.version());
        assertSame(one, service.play(game.id(), first));
        assertEquals(2, service.get(game.id(), white).version());
    }

    @Test
    void reusedIdWithChangedPayloadConflicts() {
        Game game = create();
        PlayMove first = command(white, game.legalMoves().get(0), 0);
        service.play(game.id(), first);
        fails(COMMAND_CONFLICT, () -> service.play(game.id(), new PlayMove(white,
                game.legalMoves().get(1), 0, first.clientMoveId())));
        fails(COMMAND_CONFLICT, () -> service.play(game.id(), new PlayMove(black,
                first.move(), 0, first.clientMoveId())));
        assertEquals(1, service.get(game.id(), white).version());
    }

    @Test
    void rejectedCommandDoesNotChangeStateOrConsumeId() {
        Game game = create();
        UUID id = UUID.randomUUID();
        fails(INVALID_MOVE, () -> service.play(game.id(), new PlayMove(white,
                new Move(new Square(5, 0), List.of(new Square(2, 3))), 0, id)));
        fails(WRONG_TURN, () -> service.play(game.id(), command(black, game.legalMoves().get(0), 0)));
        fails(VERSION_CONFLICT, () -> service.play(game.id(), command(white, game.legalMoves().get(0), 1)));
        fails(FORBIDDEN, () -> service.play(game.id(), command(PlayerId.newId(), game.legalMoves().get(0), 0)));
        assertEquals(0, service.get(game.id(), white).version());
        assertEquals(1, service.play(game.id(), new PlayMove(white, game.legalMoves().get(0), 0, id)).version());
        fails(NOT_FOUND, () -> service.play(GameId.newId(), command(white, game.legalMoves().get(0), 0)));
    }

    @Test
    void concurrentDifferentCommandsAcceptExactlyOne() throws Exception {
        Game game = create();
        PlayMove first = command(white, game.legalMoves().get(0), 0);
        PlayMove second = command(white, game.legalMoves().get(1), 0);
        var results = race(game.id(), first, second);
        assertEquals(1, results.stream().filter(result -> result.equals("OK")).count());
        assertEquals(1, results.stream().filter(result -> result.equals("VERSION_CONFLICT")).count());
        assertEquals(1, service.get(game.id(), white).version());
    }

    @Test
    void concurrentIdenticalCommandsAreIdempotent() throws Exception {
        Game game = create();
        PlayMove command = command(white, game.legalMoves().get(0), 0);
        assertEquals(List.of("OK", "OK"), race(game.id(), command, command));
        assertEquals(1, service.get(game.id(), white).version());
    }

    private List<String> race(GameId id, PlayMove first, PlayMove second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> submitAfter(start, id, first));
            var b = executor.submit(() -> submitAfter(start, id, second));
            start.countDown();
            return List.of(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private String submitAfter(CountDownLatch start, GameId id, PlayMove command) throws InterruptedException {
        start.await();
        try { service.play(id, command); return "OK"; }
        catch (GameFailure failure) { return failure.code().name(); }
    }
}
