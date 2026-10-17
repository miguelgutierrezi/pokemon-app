package com.miguel.pokemonapp.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.miguel.pokemonapp.support.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(PostgresTestcontainersConfiguration.class)
class FlywayBaselineIntegrationTest {

  @Test
  void flywayBaselineVersionOneIsApplied(@Autowired JdbcTemplate jdbcTemplate) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM flyway_schema_history
            WHERE version = '1' AND description = 'baseline' AND success = true
            """,
            Integer.class);

    assertThat(count).isEqualTo(1);
  }
}
