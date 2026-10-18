package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskStatus;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Persistence port. Updates must be compare-and-set on {@code (id, ownerUserId, version)}.
 */
public interface TaskRepository {

    Task saveNew(Task task);

    Optional<Task> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    /**
     * Atomically applies field changes when {@code expectedVersion} matches.
     *
     * @return updated task, or empty when no row matched (missing, not owned, or stale version —
     *     caller distinguishes stale vs missing by a prior ownership-scoped load or a dedicated
     *     outcome type in a fuller design)
     */
    Optional<Task> updateIfVersion(
            Long ownerUserId,
            Long taskId,
            long expectedVersion,
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate);

    PageResult<Task> findByOwnerUserId(Long ownerUserId, int page, int size);

    boolean deleteByIdAndOwnerUserId(Long id, Long ownerUserId);

    record PageResult<T>(java.util.List<T> content, int page, int size, long totalElements) {}
}
