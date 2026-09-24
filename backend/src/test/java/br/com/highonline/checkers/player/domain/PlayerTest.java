package br.com.highonline.checkers.player.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {
    @Test
    void registersPlayerWithItsOwnIdentity() {
        Player player = Player.register("  Rodrigo  ");
        assertNotNull(player.id());
        assertEquals("Rodrigo", player.displayName());
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThrows(IllegalArgumentException.class, () -> Player.register("   "));
    }
}
