package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class CreateTaskUseCase {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public CreateTaskUseCase(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(Long ownerUserId, Command command) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(command, "command");
        LocalDate today = LocalDate.now(clock);
        Task task = Task.create(
                ownerUserId,
                command.title(),
                command.description(),
                command.status(),
                command.dueDate(),
                today);
        return taskRepository.saveNew(task);
    }

    public record Command(
            String title, String description, TaskStatus status, LocalDate dueDate) {}
}
