package com.miguel.pokemonapp.application.port;

import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import java.util.Optional;

public interface LocalPokemonRepository {

  LocalPokemon create(LocalPokemon pokemon);

  LocalPokemon save(LocalPokemon pokemon);

  Optional<LocalPokemon> findByNationalNumber(int nationalNumber);

  Page<LocalPokemon> findPage(PageRequest request);

  boolean existsByNationalNumber(int nationalNumber);

  boolean deleteByNationalNumber(int nationalNumber);
}
