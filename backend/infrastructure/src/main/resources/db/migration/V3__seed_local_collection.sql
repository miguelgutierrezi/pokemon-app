INSERT INTO pokemon_record (
    national_number, name, category, height_m, weight_kg, image_url, generation,
    synced_at, localized_name, location, created_at, updated_at
) VALUES
(
    1,
    'Bulbasaur',
    'Seed Pokémon',
    0.7,
    6.9,
    'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png',
    'generation-i',
    TIMESTAMPTZ '2026-01-01 00:00:00+00',
    'Bulbizarre',
    'Pallet Town garden',
    TIMESTAMPTZ '2026-01-01 00:00:00+00',
    TIMESTAMPTZ '2026-01-01 00:00:00+00'
),
(
    2,
    'Ivysaur',
    'Seed Pokémon',
    1.0,
    13.0,
    'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/2.png',
    'generation-i',
    TIMESTAMPTZ '2026-01-01 00:00:00+00',
    'Herbizarre',
    'Route 1 tall grass',
    TIMESTAMPTZ '2026-01-01 00:00:00+00',
    TIMESTAMPTZ '2026-01-01 00:00:00+00'
),
(
    3,
    'Venusaur',
    'Seed Pokémon',
    2.0,
    100.0,
    'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/3.png',
    'generation-i',
    TIMESTAMPTZ '2026-01-01 00:00:00+00',
    'Florizarre',
    'Viridian Forest clearing',
    TIMESTAMPTZ '2026-01-01 00:00:00+00',
    TIMESTAMPTZ '2026-01-01 00:00:00+00'
);

INSERT INTO pokemon_record_type (record_id, slot, type)
SELECT id, 1, 'grass' FROM pokemon_record WHERE national_number = 1
UNION ALL
SELECT id, 2, 'poison' FROM pokemon_record WHERE national_number = 1
UNION ALL
SELECT id, 1, 'grass' FROM pokemon_record WHERE national_number = 2
UNION ALL
SELECT id, 2, 'poison' FROM pokemon_record WHERE national_number = 2
UNION ALL
SELECT id, 1, 'grass' FROM pokemon_record WHERE national_number = 3
UNION ALL
SELECT id, 2, 'poison' FROM pokemon_record WHERE national_number = 3;

INSERT INTO pokemon_record_tag (record_id, tag)
SELECT id, 'first-partner' FROM pokemon_record WHERE national_number = 1
UNION ALL
SELECT id, 'forest' FROM pokemon_record WHERE national_number = 1
UNION ALL
SELECT id, 'field-notes' FROM pokemon_record WHERE national_number = 2
UNION ALL
SELECT id, 'forest' FROM pokemon_record WHERE national_number = 2
UNION ALL
SELECT id, 'first-partner' FROM pokemon_record WHERE national_number = 3
UNION ALL
SELECT id, 'field-notes' FROM pokemon_record WHERE national_number = 3;
