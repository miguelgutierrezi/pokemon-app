package com.miguel.pokemonapp.domain.page;

public class PageOutOfRangeException extends RuntimeException {

  public PageOutOfRangeException(String message) {
    super(message);
  }
}
