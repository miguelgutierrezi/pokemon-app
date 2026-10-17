package com.miguel.pokemonapp.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Shared Testcontainers declarations for integration tests (Boot @ImportTestcontainers). */
public interface PostgresContainers {

  @ServiceConnection PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
}
