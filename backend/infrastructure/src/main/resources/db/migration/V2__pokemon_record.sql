CREATE TABLE pokemon_record (
    id              BIGSERIAL PRIMARY KEY,
    national_number INT          NOT NULL,
    name            VARCHAR(255) NOT NULL,
    category        VARCHAR(255),
    height_m        NUMERIC(4, 1) NOT NULL,
    weight_kg       NUMERIC(6, 1) NOT NULL,
    image_url       VARCHAR(1024),
    generation      VARCHAR(64)  NOT NULL,
    synced_at       TIMESTAMPTZ  NOT NULL,
    localized_name  VARCHAR(100),
    location        VARCHAR(200),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_pokemon_record_national_number UNIQUE (national_number)
);

CREATE TABLE pokemon_record_type (
    record_id BIGINT       NOT NULL REFERENCES pokemon_record (id) ON DELETE CASCADE,
    slot      INT          NOT NULL,
    type      VARCHAR(64)  NOT NULL,
    PRIMARY KEY (record_id, slot)
);

CREATE TABLE pokemon_record_tag (
    record_id BIGINT       NOT NULL REFERENCES pokemon_record (id) ON DELETE CASCADE,
    tag       VARCHAR(32)  NOT NULL,
    CONSTRAINT uq_pokemon_record_tag UNIQUE (record_id, tag)
);
