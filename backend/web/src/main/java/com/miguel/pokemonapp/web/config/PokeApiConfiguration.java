package com.miguel.pokemonapp.web.config;

import com.github.benmanes.caffeine.cache.Ticker;
import com.miguel.pokemonapp.application.pokemon.ListPokemonUseCase;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.infrastructure.pokeapi.PokeApiCatalogAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PokeApiProperties.class)
public class PokeApiConfiguration {

  @Bean(destroyMethod = "close")
  PokeApiCatalogAdapter pokeApiCatalogAdapter(PokeApiProperties properties) {
    return new PokeApiCatalogAdapter(
        properties.baseUrl(),
        properties.connectTimeout(),
        properties.readTimeout(),
        properties.cache().ttl(),
        properties.cache().idMaxSize(),
        properties.cache().listMaxSize(),
        Ticker.systemTicker());
  }

  @Bean
  ListPokemonUseCase listPokemonUseCase(PokemonCatalogPort catalog) {
    return new ListPokemonUseCase(catalog);
  }
}
