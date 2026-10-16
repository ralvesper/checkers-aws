package br.com.highonline.checkers.game.infrastructure.http;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.infrastructure.memory.InMemoryGameStore;
import org.springframework.context.annotation.*;

@Configuration
@Profile("local")
public class LocalGameConfiguration {
    @Bean GameStore gameStore() { return new InMemoryGameStore(); }
    @Bean GameService gameService(GameStore store) { return new GameService(store); }
}
