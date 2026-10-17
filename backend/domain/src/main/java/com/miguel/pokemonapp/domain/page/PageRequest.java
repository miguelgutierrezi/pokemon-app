package com.miguel.pokemonapp.domain.page;

public record PageRequest(int page, int size) {

  public PageRequest {
    if (page < 1) {
      throw new IllegalArgumentException("page must be >= 1");
    }
    if (size < 1 || size > 20) {
      throw new IllegalArgumentException("size must be between 1 and 20");
    }
  }

  public int offset() {
    return (page - 1) * size;
  }
}
