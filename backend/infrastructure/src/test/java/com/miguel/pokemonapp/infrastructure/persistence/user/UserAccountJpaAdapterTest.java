package com.miguel.pokemonapp.infrastructure.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miguel.pokemonapp.application.DuplicateEmailException;
import com.miguel.pokemonapp.domain.user.UserAccount;
import com.miguel.pokemonapp.infrastructure.InfrastructureTestApplication;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Persistence adapter for {@code app_user}.
 *
 * <p>Skipped: asserting that non-email {@code DataIntegrityViolationException}s are rethrown
 * unchanged. Unlike {@code LocalPokemonJpaAdapterTest}, {@link UserAccount#register} cannot bypass
 * domain length limits to force a column-limit violation, and {@code app_user} has no second unique
 * constraint to abuse. Cover that path when/if a realistic trigger exists.
 */
@DataJpaTest(
    properties = {
      "spring.jpa.open-in-view=false",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true"
    })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(classes = InfrastructureTestApplication.class)
@Testcontainers
class UserAccountJpaAdapterTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-17T12:00:00Z");
  private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUV";
  private static final String DEMO_EMAIL = "demo@pokefolio.dev";

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private UserAccountSpringDataRepository springDataRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  private UserAccountJpaAdapter adapter;
  private final List<String> createdEmails = new ArrayList<>();

  @BeforeEach
  void setUp() {
    adapter = new UserAccountJpaAdapter(springDataRepository, transactionManager);
    createdEmails.clear();
  }

  @AfterEach
  void tearDown() {
    for (String email : createdEmails) {
      jdbcTemplate.update("DELETE FROM app_user WHERE email = ?", email);
    }
    createdEmails.clear();
  }

  @Test
  void saveReturnsPersistedAccountAndFindByEmailRestoresIt() {
    String email = uniqueEmail("ash");
    UserAccount pending = UserAccount.register("  Ash  ", email, HASH, CREATED_AT);

    UserAccount saved = adapter.save(pending);
    track(saved);

    assertThat(saved.id()).isPresent();
    assertThat(saved.id().orElseThrow()).isGreaterThanOrEqualTo(1L);
    assertThat(saved.name().value()).isEqualTo("Ash");
    assertThat(saved.email().value()).isEqualTo(email);
    assertThat(saved.passwordHash()).isEqualTo(HASH);
    assertThat(saved.createdAt()).isEqualTo(CREATED_AT);

    Optional<UserAccount> found = adapter.findByEmail(email);
    assertThat(found).isPresent();
    UserAccount restored = found.orElseThrow();
    assertThat(restored.id()).isEqualTo(saved.id());
    assertThat(restored.name().value()).isEqualTo("Ash");
    assertThat(restored.email().value()).isEqualTo(email);
    assertThat(restored.passwordHash()).isEqualTo(HASH);
    assertThat(restored.createdAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void findByEmailUnknownReturnsEmpty() {
    assertThat(adapter.findByEmail("missing-" + UUID.randomUUID() + "@example.com")).isEmpty();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void secondSaveWithSameEmailDifferentCasingThrowsDuplicateEmailException() {
    String local = "ash-" + UUID.randomUUID();
    String firstEmail = local + "@pokefolio.dev";
    String secondEmail = local.toUpperCase(Locale.ROOT) + "@POKEFOLIO.DEV";

    UserAccount first = adapter.save(UserAccount.register("Ash", firstEmail, HASH, CREATED_AT));
    track(first);

    assertThatThrownBy(
            () -> adapter.save(UserAccount.register("Other", secondEmail, HASH, CREATED_AT)))
        .isInstanceOf(DuplicateEmailException.class);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentSaveOfSameEmailYieldsOneSuccessAndOneDuplicate() throws Exception {
    String email = uniqueEmail("race");
    UserAccount candidate = UserAccount.register("Racer", email, HASH, CREATED_AT);

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
                    UserAccount saved = adapter.save(candidate);
                    successes.incrementAndGet();
                    synchronized (createdEmails) {
                      createdEmails.add(saved.email().value());
                    }
                  } catch (DuplicateEmailException e) {
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
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE email = ?", Long.class, email))
        .isEqualTo(1L);
  }

  @Test
  void demoUserIsPresentAfterMigrate() {
    Optional<UserAccount> demo = adapter.findByEmail(DEMO_EMAIL);
    assertThat(demo).isPresent();
    UserAccount account = demo.orElseThrow();
    assertThat(account.name().value()).isNotBlank();
    assertThat(account.passwordHash()).isNotBlank().matches("^\\$2[aby]\\$.+");
  }

  private String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private void track(UserAccount saved) {
    createdEmails.add(saved.email().value());
  }
}
