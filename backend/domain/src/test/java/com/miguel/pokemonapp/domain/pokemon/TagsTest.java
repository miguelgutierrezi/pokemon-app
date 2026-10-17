package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TagsTest {

  @Test
  void emptyIsAllowed() {
    assertThat(Tags.empty().values()).isEmpty();
    assertThat(Tags.of(List.of()).values()).isEmpty();
  }

  @Test
  void trimsLowercasesAndDedupesPreservingFirstOccurrenceOrder() {
    Tags tags = Tags.of(List.of(" Forest ", "FIELD-NOTES", "forest", "First-Partner"));

    assertThat(tags.values()).containsExactly("forest", "field-notes", "first-partner");
  }

  @Test
  void rejectsBlankAfterTrim() {
    assertThatThrownBy(() -> Tags.of(List.of("forest", "  ")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullTag() {
    List<String> input = new ArrayList<>();
    input.add("forest");
    input.add(null);

    assertThatThrownBy(() -> Tags.of(input)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullCollection() {
    assertThatThrownBy(() -> Tags.of(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsTagOfMaxLengthThirtyTwo() {
    String tag = "a".repeat(32);

    assertThat(Tags.of(List.of(tag)).values()).containsExactly(tag);
  }

  @Test
  void rejectsTagLongerThanThirtyTwoAfterNormalisation() {
    String tag = "a".repeat(33);

    assertThatThrownBy(() -> Tags.of(List.of(tag))).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void lengthLimitAppliesToNormalisedForm() {
    // 30 letters + 2 spaces trimmed → 30 chars OK; 31 letters + spaces → 31 OK;
    // 33 letters with surrounding spaces → 33 after trim → reject
    assertThat(Tags.of(List.of("  " + "b".repeat(30) + "  ")).values())
        .containsExactly("b".repeat(30));

    assertThatThrownBy(() -> Tags.of(List.of("  " + "c".repeat(33) + "  ")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsAtMostTwentyTags() {
    List<String> twenty = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      twenty.add("tag-" + i);
    }

    assertThat(Tags.of(twenty).values()).hasSize(20);
  }

  @Test
  void rejectsMoreThanTwentyTagsAfterDedup() {
    List<String> twentyOne = new ArrayList<>();
    for (int i = 0; i < 21; i++) {
      twentyOne.add("tag-" + i);
    }

    assertThatThrownBy(() -> Tags.of(twentyOne)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void dedupeDoesNotCountDuplicatesTowardMax() {
    List<String> input = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      input.add("tag-" + i);
    }
    input.add("TAG-0"); // duplicate of first after normalise

    assertThat(Tags.of(input).values()).hasSize(20);
  }
}
