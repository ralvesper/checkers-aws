package br.com.highonline.checkers.game.infrastructure.http;

import br.com.highonline.checkers.game.application.GameFailure;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = GameController.class)
@Profile({"local", "local-postgres"})
public class GameExceptionHandler {
    @ExceptionHandler(GameFailure.class)
    public ResponseEntity<ProblemDetail> gameFailure(GameFailure failure) {
        HttpStatus status = switch (failure.code()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INVALID_MOVE -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.CONFLICT;
        };
        return problem(status, failure.code().name(), failure.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> badRequest(Exception failure) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request body, path or X-Player-Id header");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(code);
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
