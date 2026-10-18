package com.miguel.pokemonapp.web.collection;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;

/** Restores Flyway V3 seed rows 1–3 (with version 0) and deletes national_number > 3. */
public final class CollectionSeedReset {

  private static final Instant SEED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
  private static final String ARTWORK_1 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png";
  private static final String ARTWORK_2 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/2.png";
  private static final String ARTWORK_3 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/3.png";

  private CollectionSeedReset() {}

  public static void restore(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update("DELETE FROM pokemon_record WHERE national_number > 3");
    jdbcTemplate.update("DELETE FROM pokemon_record WHERE national_number IN (1, 2, 3)");
    Timestamp seedTs = Timestamp.from(SEED_INSTANT);
    jdbcTemplate.update(
        """
        INSERT INTO pokemon_record (
            national_number, name, category, height_m, weight_kg, image_url, generation,
            synced_at, localized_name, location, created_at, updated_at, version
        ) VALUES
        (1, 'Bulbasaur', 'Seed Pokémon', 0.7, 6.9, ?, 'generation-i',
         ?, 'Bulbizarre', 'Pallet Town garden', ?, ?, 0),
        (2, 'Ivysaur', 'Seed Pokémon', 1.0, 13.0, ?, 'generation-i',
         ?, 'Herbizarre', 'Route 1 tall grass', ?, ?, 0),
        (3, 'Venusaur', 'Seed Pokémon', 2.0, 100.0, ?, 'generation-i',
         ?, 'Florizarre', 'Viridian Forest clearing', ?, ?, 0)
        """,
        ARTWORK_1,
        seedTs,
        seedTs,
        seedTs,
        ARTWORK_2,
        seedTs,
        seedTs,
        seedTs,
        ARTWORK_3,
        seedTs,
        seedTs,
        seedTs);

    jdbcTemplate.update(
        """
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
        SELECT id, 2, 'poison' FROM pokemon_record WHERE national_number = 3
        """);

    jdbcTemplate.update(
        """
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
        SELECT id, 'field-notes' FROM pokemon_record WHERE national_number = 3
        """);
  }
}
