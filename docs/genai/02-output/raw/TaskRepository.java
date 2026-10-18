package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.Task;
import java.util.Optional;

/**
 * Persistence port (application layer). Implemented by infrastructure (JPA adapter).
 */
public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    PageResult<Task> findByOwnerUserId(Long ownerUserId, int page, int size);

    void delete(Task task);

    /** Simple page carrier to avoid Spring types in the application port. */
    record PageResult<T>(java.util.List<T> content, int page, int size, long totalElements) {}
}
