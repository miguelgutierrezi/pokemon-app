package com.miguel.pokemonapp.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@TestConfiguration(proxyBeanMethods = false)
@ImportTestcontainers(PostgresContainers.class)
public class PostgresTestcontainersConfiguration {}
