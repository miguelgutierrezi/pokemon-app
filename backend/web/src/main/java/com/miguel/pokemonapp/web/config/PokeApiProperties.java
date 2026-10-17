package com.miguel.pokemonapp.web.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pokemonapp.pokeapi")
public record PokeApiProperties(
    String baseUrl, Duration connectTimeout, Duration readTimeout, Cache cache) {

  public record Cache(Duration ttl, int idMaxSize, int listMaxSize) {}
}
