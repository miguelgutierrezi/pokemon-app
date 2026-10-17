package com.miguel.pokemonapp.domain.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageTest {

  @Test
  void totalPagesIsCeilOfTotalElementsOverSize() {
    Page<String> page = Page.of(List.of("a"), new PageRequest(1, 9), 1025L);

    assertThat(page.totalPages()).isEqualTo(114);
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(9);
    assertThat(page.totalElements()).isEqualTo(1025L);
    assertThat(page.content()).containsExactly("a");
  }

  @Test
  void totalPagesIsZeroWhenTotalElementsIsZero() {
    Page<String> page = Page.of(List.of(), new PageRequest(1, 9), 0L);

    assertThat(page.totalPages()).isEqualTo(0);
    assertThat(page.content()).isEmpty();
  }

  @Test
  void acceptsValidLastPage() {
    Page<String> page = Page.of(List.of("last"), new PageRequest(114, 9), 1025L);

    assertThat(page.page()).isEqualTo(114);
    assertThat(page.totalPages()).isEqualTo(114);
  }

  @Test
  void rejectsPageBeyondLastWhenTotalElementsPositive() {
    assertThatThrownBy(() -> Page.of(List.of(), new PageRequest(115, 9), 1025L))
        .isInstanceOf(PageOutOfRangeException.class);
  }

  @Test
  void compactConstructorRejectsNullContent() {
    assertThatThrownBy(() -> new Page<String>(null, 1, 9, 1025L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void compactConstructorRejectsPageBeyondLast() {
    assertThatThrownBy(() -> new Page<>(List.of(), 115, 9, 1025L))
        .isInstanceOf(PageOutOfRangeException.class);
  }

  @Test
  void rejectsNegativeTotalElements() {
    assertThatThrownBy(() -> new Page<>(List.of(), 1, 9, -1L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void compactConstructorRejectsPageLessThanOne() {
    assertThatThrownBy(() -> new Page<>(List.of(), 0, 9, 1025L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void compactConstructorRejectsSizeLessThanOne() {
    assertThatThrownBy(() -> new Page<>(List.of(), 1, 0, 1025L))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
