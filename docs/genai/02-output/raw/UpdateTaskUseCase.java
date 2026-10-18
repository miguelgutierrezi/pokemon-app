package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskNotFoundException;
import com.example.taskmgmt.domain.task.TaskStatus;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Updates a task owned by the authenticated user.
 * Missing or foreign-owned tasks → {@link TaskNotFoundException} (404).
 * Version mismatch → domain {@code OptimisticLockConflictException} (409).
 */
public final class UpdateTaskUseCase {

    private final TaskRepository taskRepository;

    public UpdateTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    public Task execute(Long ownerUserId, Long taskId, Command command) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(command, "command");

        Task task = taskRepository
                .findByIdAndOwnerUserId(taskId, ownerUserId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.update(
                command.title(),
                command.description(),
                command.status(),
                command.dueDate(),
                command.version());

        return taskRepository.save(task);
    }

    public record Command(
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            long version) {}
}
