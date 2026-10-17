package br.com.highonline.checkers.game.infrastructure.http;

import br.com.highonline.checkers.game.infrastructure.security.GuestTokens;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({"local", "local-postgres"})
public class SessionController {
    private final GuestTokens tokens;
    public SessionController(GuestTokens tokens) { this.tokens = tokens; }
    @PostMapping("/sessions")
    public ResponseEntity<GuestTokens.Issued> create() {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(tokens.issue());
    }
}
