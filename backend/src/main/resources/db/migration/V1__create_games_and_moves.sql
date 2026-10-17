CREATE TABLE games (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL CHECK (version >= 0),
    state JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((state->>'version')::bigint = version),
    CHECK ((state->>'id')::uuid = id)
);

-- Each accepted turn is also its idempotency receipt and historical snapshot.
CREATE TABLE game_moves (
    game_id UUID NOT NULL REFERENCES games(id),
    client_move_id UUID NOT NULL,
    resulting_version BIGINT NOT NULL CHECK (resulting_version > 0),
    command JSONB NOT NULL,
    result JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (game_id, client_move_id),
    UNIQUE (game_id, resulting_version),
    CHECK ((result->>'version')::bigint = resulting_version),
    CHECK ((result->>'id')::uuid = game_id)
);
