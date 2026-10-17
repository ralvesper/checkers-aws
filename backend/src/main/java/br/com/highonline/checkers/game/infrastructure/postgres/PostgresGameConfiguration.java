package br.com.highonline.checkers.game.infrastructure.postgres;

import br.com.highonline.checkers.game.application.*;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;

@Configuration
@Profile("local-postgres")
public class PostgresGameConfiguration {
    @Bean
    DataSource gameDataSource(@Value("${checkers.database.url}") String url,
                              @Value("${checkers.database.username}") String username,
                              @Value("${checkers.database.password}") String password) {
        return new DriverManagerDataSource(url, username, password);
    }

    @Bean(initMethod = "migrate")
    Flyway gameFlyway(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
    }

    @Bean
    @DependsOn("gameFlyway")
    GameStore gameStore(DataSource dataSource) {
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return new PostgresGameStore(new JdbcTemplate(dataSource), transaction);
    }

    @Bean GameService gameService(GameStore store) { return new GameService(store); }
    @Bean RoomAccessStore roomAccessStore(GameStore store, DataSource dataSource) {
        return new PostgresRoomAccess(store, new JdbcTemplate(dataSource),
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }
    @Bean RoomService roomService(RoomAccessStore access) { return new RoomService(access); }
}
