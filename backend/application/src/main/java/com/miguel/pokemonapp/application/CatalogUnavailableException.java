package com.miguel.pokemonapp.application;

public class CatalogUnavailableException extends RuntimeException {

  public CatalogUnavailableException(String message) {
    super(message);
  }

  public CatalogUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
