package br.com.highonline.checkers.game.infrastructure.security;

import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import java.security.SecureRandom;
import java.util.Base64;

@Configuration
@Profile({"local", "local-postgres"})
public class GuestSecurityConfiguration {
    @Bean GuestTokens guestTokens(Environment environment) {
        String secret = environment.getProperty("checkers.session.secret", "");
        if (secret.isBlank()) {
            if (environment.matchesProfiles("local-postgres")) throw new IllegalStateException("Set CHECKERS_SESSION_SECRET before starting persistent sessions");
            byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
            secret = Base64.getEncoder().encodeToString(bytes);
        }
        return new GuestTokens(secret);
    }
}
