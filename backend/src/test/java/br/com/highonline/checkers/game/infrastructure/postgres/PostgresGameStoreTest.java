package br.com.highonline.checkers.game.infrastructure.postgres;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

/** Runs against real PostgreSQL; CI always provides the database. */
@EnabledIfEnvironmentVariable(named = "PG_TEST_URL", matches = ".+")
class PostgresGameStoreTest {
    private static DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private GameService service;
    private final PlayerId black = PlayerId.newId(), white = PlayerId.newId();
    private final GameJsonCodec codec = new GameJsonCodec();

    @BeforeAll
    static void migrate() {
        dataSource = new DriverManagerDataSource(System.getenv("PG_TEST_URL"),
                System.getenv("PG_TEST_USER"), System.getenv("PG_TEST_PASSWORD"));
        var result = Flyway.configure().dataSource(dataSource).load().migrate();
        assertTrue(Flyway.configure().dataSource(dataSource).load().validateWithResult().validationSuccessful);
    }

    @BeforeEach
    void setup() {
        jdbc = new JdbcTemplate(dataSource);
        service = new GameService(store());
    }

    private PostgresGameStore store() {
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return new PostgresGameStore(new JdbcTemplate(dataSource), transaction);
    }

    @Test
    void stateAndReceiptSurviveNewAdapterAndReplayAfterLaterTurn() {
        Game initial = service.create(white, black, white);
        var command = new PlayMove(white, initial.legalMoves().get(0), 0, UUID.randomUUID());
        Game first = service.play(initial.id(), command);
        service.play(initial.id(), new PlayMove(black, first.legalMoves().get(0), 1, UUID.randomUUID()));

        GameService restarted = new GameService(store());
        assertEquals(2, restarted.get(initial.id(), white).version());
        assertEquals(codec.encode(first), codec.encode(restarted.play(initial.id(), command)));
        assertEquals(2, restarted.get(initial.id(), white).version());
        assertEquals(2, historyCount(initial.id()));
        var changed = new PlayMove(white, initial.legalMoves().get(1), 0, command.clientMoveId());
        assertEquals(GameFailure.Code.COMMAND_CONFLICT,
                assertThrows(GameFailure.class, () -> restarted.play(initial.id(), changed)).code());
    }

    @Test
    void concurrentDifferentCommandsCommitExactlyOneVersion() throws Exception {
        Game game = service.create(white, black, white);
        var first = new PlayMove(white, game.legalMoves().get(0), 0, UUID.randomUUID());
        var second = new PlayMove(white, game.legalMoves().get(1), 0, UUID.randomUUID());
        var outcomes = race(game.id(), first, second);
        assertEquals(1, outcomes.stream().filter("OK"::equals).count());
        assertEquals(1, outcomes.stream().filter("VERSION_CONFLICT"::equals).count());
        assertEquals(1, service.get(game.id(), white).version());
        assertEquals(1, historyCount(game.id()));
    }

    @Test
    void concurrentDuplicateCommandsReturnOriginalReceipt() throws Exception {
        Game game = service.create(white, black, white);
        var command = new PlayMove(white, game.legalMoves().get(0), 0, UUID.randomUUID());
        assertEquals(List.of("OK", "OK"), race(game.id(), command, command));
        assertEquals(1, service.get(game.id(), white).version());
        assertEquals(1, historyCount(game.id()));
    }

    @Test
    void failedHistoryInsertRollsBackGameAndAllowsRetry() {
        Game game = service.create(white, black, white);
        var command = new PlayMove(white, game.legalMoves().get(0), 0, UUID.randomUUID());
        jdbc.execute("""
                CREATE FUNCTION reject_test_move() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'injected history failure'; END $$
                """);
        jdbc.execute("CREATE TRIGGER reject_test_move BEFORE INSERT ON game_moves FOR EACH ROW EXECUTE FUNCTION reject_test_move()");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class, () -> service.play(game.id(), command));
            assertEquals(0, service.get(game.id(), white).version());
            assertEquals(0, historyCount(game.id()));
        } finally {
            jdbc.execute("DROP TRIGGER reject_test_move ON game_moves");
            jdbc.execute("DROP FUNCTION reject_test_move()");
        }
        assertEquals(1, service.play(game.id(), command).version());
        assertEquals(1, historyCount(game.id()));
    }

    @Test
    void invalidMoveDoesNotCreateHistory() {
        Game game = service.create(white, black, white);
        var command = new PlayMove(white, new Move(new Square(5, 0), List.of(new Square(2, 3))), 0, UUID.randomUUID());
        assertEquals(GameFailure.Code.INVALID_MOVE,
                assertThrows(GameFailure.class, () -> service.play(game.id(), command)).code());
        assertEquals(0, service.get(game.id(), white).version());
        assertEquals(0, historyCount(game.id()));
    }

    private int historyCount(GameId id) {
        return jdbc.queryForObject("SELECT count(*) FROM game_moves WHERE game_id = ?", Integer.class, id.value());
    }

    private List<String> race(GameId id, PlayMove first, PlayMove second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        // Separate adapters and connections, forced to read the same original snapshot before CAS.
        GameService a = new GameService(new BarrierStore(store(), barrier));
        GameService b = new GameService(new BarrierStore(store(), barrier));
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            var one = threads.submit(() -> submit(a, id, first));
            var two = threads.submit(() -> submit(b, id, second));
            return List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
        } finally { threads.shutdownNow(); }
    }

    private String submit(GameService target, GameId id, PlayMove command) {
        try { assertEquals(1, target.play(id, command).version()); return "OK"; }
        catch (GameFailure failure) { return failure.code().name(); }
    }

    private record BarrierStore(GameStore delegate, CyclicBarrier barrier) implements GameStore {
        public void create(Game game) { delegate.create(game); }
        public Optional<Game> find(GameId id) { return delegate.find(id); }
        public Game update(GameId id, Function<StoredGame, Change> operation) {
            AtomicBoolean firstRead = new AtomicBoolean(true);
            return delegate.update(id, current -> {
                if (firstRead.getAndSet(false)) {
                    try { barrier.await(10, TimeUnit.SECONDS); }
                    catch (Exception failure) { throw new IllegalStateException(failure); }
                }
                return operation.apply(current);
            });
        }
    }
}
