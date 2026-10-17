package br.com.highonline.checkers.game.infrastructure.http;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.infrastructure.memory.InMemoryGameStore;
import br.com.highonline.checkers.game.infrastructure.memory.InMemoryRoomAccess;
import org.springframework.context.annotation.*;

@Configuration
@Profile("local & !local-postgres")
public class LocalGameConfiguration {
    @Bean GameStore gameStore() { return new InMemoryGameStore(); }
    @Bean GameService gameService(GameStore store) { return new GameService(store); }
    @Bean RoomAccessStore roomAccessStore(GameStore store) { return new InMemoryRoomAccess(store); }
    @Bean RoomService roomService(RoomAccessStore access) { return new RoomService(access); }
}
