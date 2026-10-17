package br.com.highonline.checkers.game.infrastructure.realtime;

import br.com.highonline.checkers.game.application.*;
import br.com.highonline.checkers.game.domain.GameId;
import br.com.highonline.checkers.player.domain.PlayerId;
import br.com.highonline.checkers.game.infrastructure.security.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
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
    private final String[] allowedOrigins;
    private final RoomService rooms;
    private final GuestTokens tokens;
    public GameStreamConfiguration(GameStreamHandler handler, GameService service,
            @Value("${checkers.websocket.allowed-origins}") String[] allowedOrigins, RoomService rooms, GuestTokens tokens) {
        this.handler = handler; this.service = service; this.allowedOrigins = allowedOrigins;
        this.rooms = rooms; this.tokens = tokens;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/games/*/stream")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(new HandshakeInterceptor() {
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler handler, Map<String, Object> attributes) {
                        try {
                            String[] path = request.getURI().getPath().split("/");
                            GameId gameId = new GameId(UUID.fromString(path[path.length - 2]));
                            String protocols = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
                            String token = protocols == null ? null : Arrays.stream(protocols.split(","))
                                    .map(String::trim).filter(value -> value.startsWith("guest."))
                                    .map(value -> value.substring(6)).findFirst().orElse(null);
                            PlayerId playerId = rooms.seat(gameId, tokens.verify(token));
                            service.get(gameId, playerId);
                            attributes.put("gameId", gameId); attributes.put("playerId", playerId);
                            return true;
                        } catch (Unauthorized invalid) { response.setStatusCode(HttpStatus.UNAUTHORIZED); return false;
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
