package com.miguel.pokemonapp.infrastructure.persistence.user;

import com.miguel.pokemonapp.application.DuplicateEmailException;
import com.miguel.pokemonapp.application.port.UserAccountRepository;
import com.miguel.pokemonapp.domain.user.UserAccount;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class UserAccountJpaAdapter implements UserAccountRepository {

  static final String EMAIL_UNIQUE_CONSTRAINT = "uq_app_user_email";

  private final UserAccountSpringDataRepository repository;
  private final TransactionTemplate writeTx;
  private final TransactionTemplate readTx;

  public UserAccountJpaAdapter(
      UserAccountSpringDataRepository repository, PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.writeTx = new TransactionTemplate(transactionManager);
    this.readTx = new TransactionTemplate(transactionManager);
    this.readTx.setReadOnly(true);
  }

  @Override
  public UserAccount save(UserAccount account) {
    return writeTx.execute(status -> doSave(account));
  }

  @Override
  public Optional<UserAccount> findByEmail(String normalisedEmail) {
    return readTx.execute(status -> repository.findByEmail(normalisedEmail).map(this::toDomain));
  }

  private UserAccount doSave(UserAccount account) {
    try {
      UserAccountEntity entity = new UserAccountEntity();
      entity.setName(account.name().value());
      entity.setEmail(account.email().value());
      entity.setPasswordHash(account.passwordHash());
      entity.setCreatedAt(account.createdAt());
      UserAccountEntity persisted = repository.saveAndFlush(entity);
      return toDomain(persisted);
    } catch (DataIntegrityViolationException ex) {
      if (isEmailUniqueViolation(ex)) {
        throw new DuplicateEmailException(account.email().value());
      }
      throw ex;
    }
  }

  private UserAccount toDomain(UserAccountEntity entity) {
    return UserAccount.restore(
        entity.getId(),
        entity.getName(),
        entity.getEmail(),
        entity.getPasswordHash(),
        entity.getCreatedAt());
  }

  private static boolean isEmailUniqueViolation(DataIntegrityViolationException ex) {
    for (Throwable cursor = ex; cursor != null; cursor = cursor.getCause()) {
      if (cursor instanceof ConstraintViolationException constraintViolation) {
        String name = constraintViolation.getConstraintName();
        if (name != null && EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(name)) {
          return true;
        }
      }
    }
    return false;
  }
}
