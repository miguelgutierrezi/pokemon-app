package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.OptimisticLockConflictException;
import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskNotFoundException;
import com.example.taskmgmt.domain.task.TaskStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Updates a task owned by the authenticated user.
 * Missing or foreign-owned → {@link TaskNotFoundException} (404).
 * Version mismatch → {@link OptimisticLockConflictException} (409).
 */
public final class UpdateTaskUseCase {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public UpdateTaskUseCase(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(Long ownerUserId, Long taskId, Command command) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(command, "command");

        LocalDate today = LocalDate.now(clock);

        // Ownership probe: foreign or missing → same 404 (no existence leak).
        Task current = taskRepository
                .findByIdAndOwnerUserId(taskId, ownerUserId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (current.getVersion() != command.version()) {
            throw new OptimisticLockConflictException(
                    taskId, command.version(), current.getVersion());
        }

        // Unchanged past dueDate allowed; changed dueDate must be >= today.
        Task candidate = current.withUpdate(
                command.title(),
                command.description(),
                command.status(),
                command.dueDate(),
                today);

        return taskRepository
                .updateIfVersion(
                        ownerUserId,
                        taskId,
                        command.version(),
                        candidate.getTitle(),
                        candidate.getDescription(),
                        candidate.getStatus(),
                        candidate.getDueDate())
                .orElseThrow(
                        () ->
                                new OptimisticLockConflictException(
                                        taskId, command.version(), current.getVersion()));
    }

    public record Command(
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            long version) {}
}
