package br.com.highonline.checkers.game.infrastructure.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;

/** Versioned, HMAC-authenticated guest capability. Not an account or an identity provider. */
public final class GuestTokens {
    private final byte[] secret;
    private final Clock clock;
    private static final Base64.Encoder ENCODE = Base64.getUrlEncoder().withoutPadding();
    public GuestTokens(String secret) { this(secret, Clock.systemUTC()); }
    public GuestTokens(String secret, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32 || secret.startsWith("replace-")) {
            throw new IllegalArgumentException("Session signing secret must contain at least 32 random bytes");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8); this.clock = clock;
    }
    public record Issued(String token, long expiresAt) {}
    public Issued issue() {
        long expires = clock.instant().getEpochSecond() + 30L * 86400;
        String body = ENCODE.encodeToString(("v1:" + UUID.randomUUID() + ":" + expires).getBytes(StandardCharsets.UTF_8));
        return new Issued(body + "." + ENCODE.encodeToString(mac(body)), expires);
    }
    public UUID authorization(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new Unauthorized();
        return verify(header.substring(7));
    }
    public UUID verify(String token) {
        try {
            if (token == null || token.length() > 512) throw new Unauthorized();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(mac(parts[0]), Base64.getUrlDecoder().decode(parts[1]))) throw new Unauthorized();
            String[] claims = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8).split(":", -1);
            if (claims.length != 3 || !claims[0].equals("v1") || Long.parseLong(claims[2]) <= clock.instant().getEpochSecond()) throw new Unauthorized();
            return UUID.fromString(claims[1]);
        } catch (IllegalArgumentException failure) { throw new Unauthorized(); }
    }
    private byte[] mac(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException impossible) { throw new IllegalStateException(impossible); }
    }
}
