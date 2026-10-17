package br.com.highonline.checkers.game.application;

import br.com.highonline.checkers.game.domain.*;
import br.com.highonline.checkers.player.domain.PlayerId;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

public final class RoomService {
    private final RoomAccessStore access;
    private final SecureRandom random = new SecureRandom();
    public RoomService(RoomAccessStore access) { this.access = access; }
    public record Created(Game game, String invitation) {}

    public Created create(UUID session) {
        Game game = Game.start(PlayerId.newId(), PlayerId.newId());
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String invitation = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        access.create(game, session, hash(invitation), Instant.now().plusSeconds(7 * 86400));
        return new Created(game, invitation);
    }
    public PlayerId seat(GameId id, UUID session) { return access.seat(id, session); }
    public PlayerId join(GameId id, UUID session, String invitation) {
        if (invitation == null || invitation.length() < 40 || invitation.length() > 128) {
            throw new IllegalArgumentException("Invalid invitation");
        }
        return access.join(id, session, hash(invitation), Instant.now());
    }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
