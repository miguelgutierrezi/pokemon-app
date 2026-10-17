package com.miguel.pokemonapp.application;

public class DuplicateLocalPokemonException extends RuntimeException {

  private final int nationalNumber;

  public DuplicateLocalPokemonException(int nationalNumber) {
    super("Pokémon #" + nationalNumber + " is already in the local collection");
    this.nationalNumber = nationalNumber;
  }

  public int nationalNumber() {
    return nationalNumber;
  }
}
