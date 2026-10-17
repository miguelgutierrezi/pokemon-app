package com.miguel.pokemonapp.web.config;

import com.miguel.pokemonapp.application.pokemon.GetLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ImportPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ListLocalPokemonUseCase;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.infrastructure.persistence.LocalPokemonJpaAdapter;
import com.miguel.pokemonapp.infrastructure.persistence.PokemonRecordSpringDataRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CollectionConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  LocalPokemonJpaAdapter localPokemonJpaAdapter(
      PokemonRecordSpringDataRepository springDataRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    return new LocalPokemonJpaAdapter(springDataRepository, clock, transactionManager);
  }

  @Bean
  ImportPokemonUseCase importPokemonUseCase(
      LocalPokemonRepository repository, PokemonCatalogPort catalog, Clock clock) {
    return new ImportPokemonUseCase(repository, catalog, clock);
  }

  @Bean
  ListLocalPokemonUseCase listLocalPokemonUseCase(LocalPokemonRepository repository) {
    return new ListLocalPokemonUseCase(repository);
  }

  @Bean
  GetLocalPokemonUseCase getLocalPokemonUseCase(LocalPokemonRepository repository) {
    return new GetLocalPokemonUseCase(repository);
  }
}
