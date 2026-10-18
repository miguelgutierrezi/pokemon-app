-- Demo password hash generated with:
--   htpasswd -bnBC 10 "" 'Demo-Passw0rd!' | tr -d ':\n'
-- Plaintext demo credential (not a production secret): Demo-Passw0rd!

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_app_user_email UNIQUE (email)
);

INSERT INTO app_user (name, email, password_hash, created_at)
VALUES (
    'Demo Trainer',
    'demo@pokefolio.dev',
    '$2y$10$QbN1EUHsuTiDjWAKtwnmv.A7fs2gE8Zgy0gU/RI3yf3irF8NtRySG',
    TIMESTAMPTZ '2026-01-01T00:00:00Z'
);
