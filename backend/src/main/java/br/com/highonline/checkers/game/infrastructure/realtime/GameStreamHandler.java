package br.com.highonline.checkers.game.infrastructure.realtime;

import br.com.highonline.checkers.game.application.GameService;
import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.game.infrastructure.http.GameController.GameResponse;
import br.com.highonline.checkers.player.domain.PlayerId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Best-effort confirmed snapshots for one application instance. REST remains authoritative. */
@Component
@Profile({"local", "local-postgres"})
public class GameStreamHandler extends TextWebSocketHandler {
    private final GameService service;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
    public GameStreamHandler(GameService service) { this.service = service; }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        GameId gameId = (GameId) session.getAttributes().get("gameId");
        PlayerId playerId = (PlayerId) session.getAttributes().get("playerId");
        Client client = new Client(gameId, playerId, new ConcurrentWebSocketSessionDecorator(session, 5000, 128 * 1024));
        clients.put(session.getId(), client);
        try { send(client, service.get(gameId, playerId)); }
        catch (Exception failure) { clients.remove(session.getId()); session.close(CloseStatus.POLICY_VIOLATION); }
    }

    /** Call only after the store transaction has returned successfully. Repeated versions are harmless. */
    public void publish(Game game) {
        clients.forEach((id, client) -> {
            if (client.gameId().equals(game.id())) {
                try { send(client, game); }
                catch (Exception failure) {
                    clients.remove(id);
                    try { client.session().close(CloseStatus.SERVER_ERROR); } catch (Exception ignored) { }
                }
            }
        });
    }

    private void send(Client client, Game game) throws Exception {
        if (client.session().isOpen()) client.session().sendMessage(new TextMessage(mapper.writeValueAsString(GameResponse.from(game))));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { clients.remove(session.getId()); }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable failure) throws Exception {
        clients.remove(session.getId()); session.close(CloseStatus.SERVER_ERROR);
    }

    private record Client(GameId gameId, PlayerId playerId, WebSocketSession session) {}
}
