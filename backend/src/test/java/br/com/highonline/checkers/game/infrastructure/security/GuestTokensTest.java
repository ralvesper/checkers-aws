package br.com.highonline.checkers.game.infrastructure.security;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class GuestTokensTest {
    private final String secret = "test-only-0123456789abcdef0123456789abcdef";
    private final Instant now = Instant.parse("2026-10-17T00:00:00Z");
    @Test void verifiesIssuedTokenAcrossInstancesWithSameSecret() {
        GuestTokens tokens = new GuestTokens(secret, Clock.fixed(now, ZoneOffset.UTC));
        String token = tokens.issue().token();
        UUID session = tokens.authorization("Bearer " + token);
        assertEquals(session, new GuestTokens(secret, Clock.fixed(now, ZoneOffset.UTC)).verify(token));
        assertNotEquals(session, tokens.verify(tokens.issue().token()));
    }
    @Test void rejectsTamperingRawIdsMissingHeadersAndOtherSigningKey() {
        GuestTokens tokens = new GuestTokens(secret, Clock.fixed(now, ZoneOffset.UTC));
        String token = tokens.issue().token();
        assertThrows(Unauthorized.class, () -> tokens.verify(token + "x"));
        assertThrows(Unauthorized.class, () -> tokens.verify(UUID.randomUUID().toString()));
        assertThrows(Unauthorized.class, () -> tokens.authorization(null));
        assertThrows(Unauthorized.class, () -> tokens.authorization("Basic " + token));
        assertThrows(Unauthorized.class, () -> new GuestTokens(secret + "other").verify(token));
    }
    @Test void rejectsExpiredToken() {
        String token = new GuestTokens(secret, Clock.fixed(now, ZoneOffset.UTC)).issue().token();
        assertThrows(Unauthorized.class, () -> new GuestTokens(secret,
                Clock.fixed(now.plusSeconds(31L * 86400), ZoneOffset.UTC)).verify(token));
    }
    @Test void rejectsShortOrPlaceholderSecret() {
        assertThrows(IllegalArgumentException.class, () -> new GuestTokens("short"));
        assertThrows(IllegalArgumentException.class, () -> new GuestTokens("replace-with-at-least-32-random-bytes"));
    }
}
