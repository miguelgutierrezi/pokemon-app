package com.miguel.pokemonapp.domain.page;

public record PageRequest(int page, int size) {

  public static final int DEFAULT_SIZE = 9;
  public static final int MAX_SIZE = 20;

  public PageRequest {
    if (page < 1) {
      throw new IllegalArgumentException("page must be >= 1");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
    }
  }

  public int offset() {
    return (page - 1) * size;
  }

  public void requireWithin(long totalElements) {
    if (totalElements < 0) {
      throw new IllegalArgumentException("totalElements must be >= 0");
    }
    if (totalElements == 0) {
      return;
    }
    int totalPages = PageCounts.pagesFor(totalElements, size);
    if (page > totalPages) {
      throw new PageOutOfRangeException("page " + page + " is beyond last page " + totalPages);
    }
  }
}
