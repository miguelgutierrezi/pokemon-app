package com.miguel.pokemonapp.domain.page;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements) {

  public Page {
    if (content == null) {
      throw new IllegalArgumentException("content must not be null");
    }
    if (page < 1) {
      throw new IllegalArgumentException("page must be >= 1");
    }
    if (size < 1) {
      throw new IllegalArgumentException("size must be >= 1");
    }
    if (totalElements < 0) {
      throw new IllegalArgumentException("totalElements must be >= 0");
    }
    content = List.copyOf(content);
    int pages = PageCounts.pagesFor(totalElements, size);
    if (totalElements > 0 && page > pages) {
      throw new PageOutOfRangeException("page " + page + " is beyond last page " + pages);
    }
  }

  public int totalPages() {
    return PageCounts.pagesFor(totalElements, size);
  }

  public static <T> Page<T> of(List<T> content, PageRequest request, long totalElements) {
    return new Page<>(content, request.page(), request.size(), totalElements);
  }
}
