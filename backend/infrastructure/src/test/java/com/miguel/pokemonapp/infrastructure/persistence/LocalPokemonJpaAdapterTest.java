package com.miguel.pokemonapp.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miguel.pokemonapp.application.DuplicateLocalPokemonException;
import com.miguel.pokemonapp.application.StaleLocalPokemonException;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageOutOfRangeException;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.Tags;
import com.miguel.pokemonapp.infrastructure.InfrastructureTestApplication;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest(
    properties = {
      "spring.jpa.open-in-view=false",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true"
    })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(classes = InfrastructureTestApplication.class)
@Testcontainers
class LocalPokemonJpaAdapterTest {

  private static final Instant SYNCED_AT = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant CREATED_AT = Instant.parse("2026-06-15T12:00:00Z");
  private static final String ARTWORK_1 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png";
  private static final String ARTWORK_2 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/2.png";
  private static final String ARTWORK_3 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/3.png";

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private PokemonRecordSpringDataRepository springDataRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private EntityManager entityManager;

  private LocalPokemonJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
    adapter = new LocalPokemonJpaAdapter(springDataRepository, clock, transactionManager);
    springDataRepository.findAll().stream()
        .filter(entity -> entity.getNationalNumber() > 3)
        .forEach(springDataRepository::delete);
    springDataRepository.flush();
  }

  @Test
  void saveAndFindRoundTripPreservesTypeOrderAndTags() {
    LocalPokemon saved =
        adapter.create(
            new LocalPokemon(
                25,
                "Pikachu",
                "Mouse Pokémon",
                List.of("electric"),
                new BigDecimal("0.4"),
                new BigDecimal("6.0"),
                ARTWORK_1.replace("/1.png", "/25.png"),
                "generation-i",
                SYNCED_AT,
                null,
                null,
                Tags.of(List.of(" Field-Notes ", "forest", "FOREST")),
                0L));

    assertThat(saved.nationalNumber()).isEqualTo(25);
    assertThat(saved.types()).containsExactly("electric");
    assertThat(saved.tags().values()).containsExactly("field-notes", "forest");

    LocalPokemon found = adapter.findByNationalNumber(25).orElseThrow();
    assertThat(found.name()).isEqualTo("Pikachu");
    assertThat(found.types()).containsExactly("electric");
    assertThat(found.tags().values()).containsExactly("field-notes", "forest");
    assertThat(found.syncedAt()).isEqualTo(SYNCED_AT);

    PokemonRecordEntity persisted = springDataRepository.findByNationalNumber(25).orElseThrow();
    assertThat(persisted.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(persisted.getUpdatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void findPageOrdersByNationalNumberAndEnforcesPageBeyondLast() {
    Page<LocalPokemon> page = adapter.findPage(new PageRequest(1, 2));

    assertThat(page.content()).extracting(LocalPokemon::nationalNumber).containsExactly(1, 2);
    assertThat(page.totalElements()).isEqualTo(3L);
    assertThat(page.totalPages()).isEqualTo(2);

    Page<LocalPokemon> second = adapter.findPage(new PageRequest(2, 2));
    assertThat(second.content()).extracting(LocalPokemon::nationalNumber).containsExactly(3);

    assertThatThrownBy(() -> adapter.findPage(new PageRequest(3, 2)))
        .isInstanceOf(PageOutOfRangeException.class);
  }

  @Test
  void seedRowsPresentWithExactValues() {
    LocalPokemon bulbasaur = adapter.findByNationalNumber(1).orElseThrow();
    assertThat(bulbasaur.name()).isEqualTo("Bulbasaur");
    assertThat(bulbasaur.category()).isEqualTo("Seed Pokémon");
    assertThat(bulbasaur.types()).containsExactly("grass", "poison");
    assertThat(bulbasaur.heightM()).isEqualByComparingTo("0.7");
    assertThat(bulbasaur.weightKg()).isEqualByComparingTo("6.9");
    assertThat(bulbasaur.imageUrl()).isEqualTo(ARTWORK_1);
    assertThat(bulbasaur.generation()).isEqualTo("generation-i");
    assertThat(bulbasaur.syncedAt()).isEqualTo(SYNCED_AT);
    assertThat(bulbasaur.localizedName()).isEqualTo("Bulbizarre");
    assertThat(bulbasaur.location()).isEqualTo("Pallet Town garden");
    assertThat(bulbasaur.tags().values()).containsExactlyInAnyOrder("first-partner", "forest");
    assertThat(bulbasaur.version()).isZero();

    LocalPokemon ivysaur = adapter.findByNationalNumber(2).orElseThrow();
    assertThat(ivysaur.name()).isEqualTo("Ivysaur");
    assertThat(ivysaur.heightM()).isEqualByComparingTo("1.0");
    assertThat(ivysaur.weightKg()).isEqualByComparingTo("13.0");
    assertThat(ivysaur.imageUrl()).isEqualTo(ARTWORK_2);
    assertThat(ivysaur.localizedName()).isEqualTo("Herbizarre");
    assertThat(ivysaur.location()).isEqualTo("Route 1 tall grass");
    assertThat(ivysaur.tags().values()).containsExactlyInAnyOrder("field-notes", "forest");

    LocalPokemon venusaur = adapter.findByNationalNumber(3).orElseThrow();
    assertThat(venusaur.name()).isEqualTo("Venusaur");
    assertThat(venusaur.heightM()).isEqualByComparingTo("2.0");
    assertThat(venusaur.weightKg()).isEqualByComparingTo("100.0");
    assertThat(venusaur.imageUrl()).isEqualTo(ARTWORK_3);
    assertThat(venusaur.localizedName()).isEqualTo("Florizarre");
    assertThat(venusaur.location()).isEqualTo("Viridian Forest clearing");
    assertThat(venusaur.tags().values()).containsExactlyInAnyOrder("first-partner", "field-notes");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void nonUniqueConstraintIntegrityViolationIsNotReportedAsDuplicate() {
    String tooLongName = "n".repeat(300);

    assertThatThrownBy(
            () ->
                adapter.create(
                    new LocalPokemon(
                        25,
                        tooLongName,
                        "Mouse Pokémon",
                        List.of("electric"),
                        new BigDecimal("0.4"),
                        new BigDecimal("6.0"),
                        null,
                        "generation-i",
                        SYNCED_AT,
                        null,
                        null,
                        Tags.empty(),
                        0L)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .isNotInstanceOf(DuplicateLocalPokemonException.class);

    assertThat(adapter.existsByNationalNumber(25)).isFalse();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Commit
  void concurrentSaveOfSameNationalNumberYieldsOneRowAndOneDuplicate() throws Exception {
    LocalPokemon candidate =
        new LocalPokemon(
            150,
            "Mewtwo",
            "Genetic Pokémon",
            List.of("psychic"),
            new BigDecimal("2.0"),
            new BigDecimal("122.0"),
            null,
            "generation-i",
            SYNCED_AT,
            null,
            null,
            Tags.empty(),
            0L);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger duplicates = new AtomicInteger();
    List<Throwable> unexpected = new ArrayList<>();

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        futures.add(
            pool.submit(
                () -> {
                  try {
                    ready.countDown();
                    start.await();
                    adapter.create(candidate);
                    successes.incrementAndGet();
                  } catch (DuplicateLocalPokemonException e) {
                    duplicates.incrementAndGet();
                  } catch (Throwable t) {
                    synchronized (unexpected) {
                      unexpected.add(t);
                    }
                  }
                }));
      }

      ready.await();
      start.countDown();
      for (Future<?> future : futures) {
        future.get();
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(unexpected).isEmpty();
    assertThat(successes.get()).isEqualTo(1);
    assertThat(duplicates.get()).isEqualTo(1);
    assertThat(springDataRepository.countByNationalNumber(150)).isEqualTo(1L);

    springDataRepository.findByNationalNumber(150).ifPresent(springDataRepository::delete);
    springDataRepository.flush();
  }

  @Test
  void updateRoundTripIncrementsVersionAdvancesUpdatedAtKeepsSyncedAt() {
    LocalPokemon current = adapter.findByNationalNumber(1).orElseThrow();
    Instant syncedAt = current.syncedAt();
    Instant updatedBefore =
        springDataRepository.findByNationalNumber(1).orElseThrow().getUpdatedAt();

    Instant later = CREATED_AT.plusSeconds(120);
    LocalPokemonJpaAdapter laterAdapter =
        new LocalPokemonJpaAdapter(
            springDataRepository, Clock.fixed(later, ZoneOffset.UTC), transactionManager);

    LocalPokemon updated =
        laterAdapter.save(
            current.withProprietary("Updated name", "Updated loc", Tags.of(List.of("updated"))));

    assertThat(updated.version()).isEqualTo(1L);
    assertThat(updated.syncedAt()).isEqualTo(syncedAt);
    assertThat(updated.localizedName()).isEqualTo("Updated name");
    assertThat(updated.location()).isEqualTo("Updated loc");
    assertThat(updated.tags().values()).containsExactly("updated");
    assertThat(updated.name()).isEqualTo("Bulbasaur");
    assertThat(updated.types()).containsExactly("grass", "poison");

    PokemonRecordEntity persisted = springDataRepository.findByNationalNumber(1).orElseThrow();
    assertThat(persisted.getUpdatedAt()).isEqualTo(later);
    assertThat(persisted.getUpdatedAt()).isAfter(updatedBefore);
    assertThat(persisted.getSyncedAt()).isEqualTo(syncedAt);
    assertThat(persisted.getVersion()).isEqualTo(1L);
  }

  @Test
  void tagsOnlyUpdateIncrementsVersionWhenUpdatedAtUnchanged() {
    LocalPokemon created = adapter.create(sample(25, "Pikachu"));
    assertThat(created.version()).isZero();
    Instant storedUpdatedAt =
        springDataRepository.findByNationalNumber(25).orElseThrow().getUpdatedAt();
    assertThat(storedUpdatedAt).isEqualTo(CREATED_AT);

    LocalPokemon updated =
        adapter.save(
            created.withProprietary(
                created.localizedName(), created.location(), Tags.of(List.of("only-tags"))));

    assertThat(updated.version()).isEqualTo(1L);
    assertThat(updated.tags().values()).containsExactly("only-tags");
    assertThat(updated.localizedName()).isEqualTo(created.localizedName());
    assertThat(updated.location()).isEqualTo(created.location());
    assertThat(springDataRepository.findByNationalNumber(25).orElseThrow().getUpdatedAt())
        .isEqualTo(CREATED_AT);
    assertThat(springDataRepository.findByNationalNumber(25).orElseThrow().getVersion())
        .isEqualTo(1L);
  }

  @Test
  void staleDomainVersionThrowsWithoutWriting() {
    LocalPokemon current = adapter.findByNationalNumber(1).orElseThrow();
    LocalPokemon stale =
        new LocalPokemon(
            current.nationalNumber(),
            current.name(),
            current.category(),
            current.types(),
            current.heightM(),
            current.weightKg(),
            current.imageUrl(),
            current.generation(),
            current.syncedAt(),
            current.localizedName(),
            current.location(),
            current.tags(),
            current.version() + 5);

    assertThatThrownBy(
            () -> adapter.save(stale.withProprietary("nope", "nope", Tags.of(List.of("nope")))))
        .isInstanceOf(StaleLocalPokemonException.class)
        .extracting(ex -> ((StaleLocalPokemonException) ex).nationalNumber())
        .isEqualTo(1);

    LocalPokemon still = adapter.findByNationalNumber(1).orElseThrow();
    assertThat(still.localizedName()).isEqualTo(current.localizedName());
    assertThat(still.version()).isEqualTo(current.version());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void saveAfterRowDeletedThrowsStaleLocalPokemonException() {
    LocalPokemon saved = adapter.create(sample(25, "Pikachu"));
    LocalPokemon loaded = adapter.findByNationalNumber(25).orElseThrow();
    assertThat(loaded.version()).isEqualTo(saved.version());

    assertThat(adapter.deleteByNationalNumber(25)).isTrue();

    assertThatThrownBy(
            () ->
                adapter.save(loaded.withProprietary("ghost", "nowhere", Tags.of(List.of("gone")))))
        .isInstanceOf(StaleLocalPokemonException.class)
        .extracting(ex -> ((StaleLocalPokemonException) ex).nationalNumber())
        .isEqualTo(25);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Commit
  void concurrentUpdateBSavesOnlyAfterACommitsYieldsOneStale() throws Exception {
    adapter.create(sample(25, "Pikachu"));
    LocalPokemon aLoaded = adapter.findByNationalNumber(25).orElseThrow();
    LocalPokemon bLoaded = adapter.findByNationalNumber(25).orElseThrow();
    assertThat(aLoaded.version()).isZero();
    assertThat(bLoaded.version()).isZero();

    CountDownLatch aCommitted = new CountDownLatch(1);
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger stales = new AtomicInteger();
    List<Throwable> unexpected = new ArrayList<>();

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<?> a =
          pool.submit(
              () -> {
                try {
                  adapter.save(
                      aLoaded.withProprietary("A-name", "A-loc", Tags.of(List.of("a-writer"))));
                  successes.incrementAndGet();
                } catch (StaleLocalPokemonException e) {
                  stales.incrementAndGet();
                } catch (Throwable t) {
                  synchronized (unexpected) {
                    unexpected.add(t);
                  }
                } finally {
                  aCommitted.countDown();
                }
              });
      Future<?> b =
          pool.submit(
              () -> {
                try {
                  aCommitted.await();
                  adapter.save(
                      bLoaded.withProprietary("B-name", "B-loc", Tags.of(List.of("b-writer"))));
                  successes.incrementAndGet();
                } catch (StaleLocalPokemonException e) {
                  stales.incrementAndGet();
                } catch (Throwable t) {
                  synchronized (unexpected) {
                    unexpected.add(t);
                  }
                }
              });
      a.get();
      b.get();
    } finally {
      pool.shutdownNow();
    }

    assertThat(unexpected).isEmpty();
    assertThat(successes.get()).isEqualTo(1);
    assertThat(stales.get()).isEqualTo(1);

    LocalPokemon persisted = adapter.findByNationalNumber(25).orElseThrow();
    assertThat(persisted.localizedName()).isEqualTo("A-name");
    assertThat(persisted.location()).isEqualTo("A-loc");
    assertThat(persisted.tags().values()).containsExactly("a-writer");
    assertThat(persisted.version()).isEqualTo(1L);

    adapter.deleteByNationalNumber(25);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @Commit
  void concurrentTagsOnlyUpdateBSavesOnlyAfterACommitsYieldsOneStale() throws Exception {
    // Same race as above, but both writers change ONLY the tags: the inverse collection does not
    // dirty the parent, so this path must still be version-guarded (compare-and-set UPDATE).
    LocalPokemon created = adapter.create(sample(25, "Pikachu"));
    LocalPokemon aLoaded = adapter.findByNationalNumber(25).orElseThrow();
    LocalPokemon bLoaded = adapter.findByNationalNumber(25).orElseThrow();
    assertThat(aLoaded.version()).isZero();
    assertThat(bLoaded.version()).isZero();

    CountDownLatch aCommitted = new CountDownLatch(1);
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger stales = new AtomicInteger();
    List<Throwable> unexpected = new ArrayList<>();

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<?> a =
          pool.submit(
              () -> {
                try {
                  adapter.save(
                      aLoaded.withProprietary(
                          created.localizedName(), created.location(), Tags.of(List.of("a-tag"))));
                  successes.incrementAndGet();
                } catch (StaleLocalPokemonException e) {
                  stales.incrementAndGet();
                } catch (Throwable t) {
                  synchronized (unexpected) {
                    unexpected.add(t);
                  }
                } finally {
                  aCommitted.countDown();
                }
              });
      Future<?> b =
          pool.submit(
              () -> {
                try {
                  aCommitted.await();
                  adapter.save(
                      bLoaded.withProprietary(
                          created.localizedName(), created.location(), Tags.of(List.of("b-tag"))));
                  successes.incrementAndGet();
                } catch (StaleLocalPokemonException e) {
                  stales.incrementAndGet();
                } catch (Throwable t) {
                  synchronized (unexpected) {
                    unexpected.add(t);
                  }
                }
              });
      a.get();
      b.get();
    } finally {
      pool.shutdownNow();
    }

    assertThat(unexpected).isEmpty();
    assertThat(successes.get()).isEqualTo(1);
    assertThat(stales.get()).isEqualTo(1);

    LocalPokemon persisted = adapter.findByNationalNumber(25).orElseThrow();
    assertThat(persisted.tags().values()).containsExactly("a-tag");
    assertThat(persisted.version()).isEqualTo(1L);

    adapter.deleteByNationalNumber(25);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void deleteByNationalNumberReturnsTrueFalseAndCascadesChildren() {
    adapter.create(
        new LocalPokemon(
            25,
            "Pikachu",
            "Mouse Pokémon",
            List.of("electric", "steel"),
            new BigDecimal("0.4"),
            new BigDecimal("6.0"),
            null,
            "generation-i",
            SYNCED_AT,
            null,
            null,
            Tags.of(List.of("forest", "field-notes")),
            0L));

    Long recordId = springDataRepository.findByNationalNumber(25).orElseThrow().getId();
    assertThat(countTypesForRecord(recordId)).isEqualTo(2L);
    assertThat(countTagsForRecord(recordId)).isEqualTo(2L);

    assertThat(adapter.deleteByNationalNumber(25)).isTrue();
    assertThat(adapter.deleteByNationalNumber(25)).isFalse();
    assertThat(adapter.findByNationalNumber(25)).isEmpty();
    assertThat(countTypesForRecord(recordId)).isZero();
    assertThat(countTagsForRecord(recordId)).isZero();
    assertThat(countOrphanTypes()).isZero();
    assertThat(countOrphanTags()).isZero();
  }

  private long countTypesForRecord(long recordId) {
    return ((Number)
            entityManager
                .createNativeQuery("select count(*) from pokemon_record_type where record_id = :id")
                .setParameter("id", recordId)
                .getSingleResult())
        .longValue();
  }

  private long countTagsForRecord(long recordId) {
    return ((Number)
            entityManager
                .createNativeQuery("select count(*) from pokemon_record_tag where record_id = :id")
                .setParameter("id", recordId)
                .getSingleResult())
        .longValue();
  }

  private long countOrphanTypes() {
    return ((Number)
            entityManager
                .createNativeQuery(
                    """
                    select count(*) from pokemon_record_type t
                    where not exists (
                      select 1 from pokemon_record r where r.id = t.record_id)
                    """)
                .getSingleResult())
        .longValue();
  }

  private long countOrphanTags() {
    return ((Number)
            entityManager
                .createNativeQuery(
                    """
                    select count(*) from pokemon_record_tag t
                    where not exists (
                      select 1 from pokemon_record r where r.id = t.record_id)
                    """)
                .getSingleResult())
        .longValue();
  }

  private static LocalPokemon sample(int nationalNumber, String name) {
    return new LocalPokemon(
        nationalNumber,
        name,
        "Mouse Pokémon",
        List.of("electric"),
        new BigDecimal("0.4"),
        new BigDecimal("6.0"),
        ARTWORK_1.replace("/1.png", "/" + nationalNumber + ".png"),
        "generation-i",
        SYNCED_AT,
        null,
        null,
        Tags.empty(),
        0L);
  }
}
