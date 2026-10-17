-- Existing games are preserved, but legacy UUID-only links grant no access.
CREATE TABLE game_access (
    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    player_id UUID NOT NULL,
    side VARCHAR(5) NOT NULL CHECK (side IN ('WHITE', 'BLACK')),
    session_id UUID,
    invitation_hash VARCHAR(64),
    invitation_expires_at TIMESTAMPTZ,
    PRIMARY KEY (game_id, player_id),
    UNIQUE (game_id, side),
    UNIQUE (game_id, session_id)
);
