package br.com.highonline.checkers.game.infrastructure.realtime;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.GameId;
import br.com.highonline.checkers.player.domain.PlayerId;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.http.server.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;

@Configuration
@EnableWebSocket
@Profile({"local", "local-postgres"})
public class GameStreamConfiguration implements WebSocketConfigurer {
    private final GameStreamHandler handler;
    private final GameService service;
    public GameStreamConfiguration(GameStreamHandler handler, GameService service) {
        this.handler = handler; this.service = service;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/games/*/stream")
                .setAllowedOrigins("http://localhost:4200", "http://127.0.0.1:4200")
                .addInterceptors(new HandshakeInterceptor() {
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler handler, Map<String, Object> attributes) {
                        try {
                            String[] path = request.getURI().getPath().split("/");
                            GameId gameId = new GameId(UUID.fromString(path[path.length - 2]));
                            String actor = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("playerId");
                            PlayerId playerId = new PlayerId(UUID.fromString(actor));
                            service.get(gameId, playerId);
                            attributes.put("gameId", gameId); attributes.put("playerId", playerId);
                            return true;
                        } catch (GameFailure failure) {
                            response.setStatusCode(failure.code() == GameFailure.Code.NOT_FOUND ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN);
                            return false;
                        } catch (Exception invalid) { response.setStatusCode(HttpStatus.BAD_REQUEST); return false; }
                    }
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler handler, Exception exception) { }
                });
    }
}
