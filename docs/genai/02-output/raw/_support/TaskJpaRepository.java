package com.example.taskmgmt.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA interface (infrastructure). The adapter maps {@code TaskEntity} ↔ domain {@code Task}.
 */
public interface TaskJpaRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    Page<TaskEntity> findByOwnerUserId(Long ownerUserId, Pageable pageable);
}
