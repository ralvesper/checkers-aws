package br.com.highonline.checkers.game.application;

import br.com.highonline.checkers.game.infrastructure.memory.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RoomServiceTest {
    @Test void bindsSeatsToSessionsAndConsumesInvitation() throws Exception {
        var rooms = new RoomService(new InMemoryRoomAccess(new InMemoryGameStore()));
        UUID owner = UUID.randomUUID(), guest = UUID.randomUUID();
        var created = rooms.create(owner);
        assertEquals(created.game().whitePlayerId(), rooms.seat(created.game().id(), owner));
        assertThrows(GameFailure.class, () -> rooms.seat(created.game().id(), created.game().blackPlayerId().value()));
        assertThrows(GameFailure.class, () -> rooms.join(created.game().id(), owner, created.invitation()));
        assertEquals(created.game().blackPlayerId(), rooms.join(created.game().id(), guest, created.invitation()));
        assertEquals(created.game().blackPlayerId(), rooms.join(created.game().id(), guest, created.invitation()));
        assertThrows(GameFailure.class, () -> rooms.join(created.game().id(), UUID.randomUUID(), created.invitation()));
    }
}
