package com.miguel.pokemonapp.domain.page;

final class PageCounts {

  private PageCounts() {}

  static int pagesFor(long totalElements, int size) {
    return Math.toIntExact(Math.ceilDiv(totalElements, size));
  }
}
