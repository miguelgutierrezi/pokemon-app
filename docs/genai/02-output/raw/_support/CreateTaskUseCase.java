package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskStatus;
import java.time.LocalDate;
import java.util.Objects;

public final class CreateTaskUseCase {

    private final TaskRepository taskRepository;

    public CreateTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    public Task execute(Long ownerUserId, Command command) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(command, "command");
        Task task = Task.create(
                ownerUserId,
                command.title(),
                command.description(),
                command.status(),
                command.dueDate());
        return taskRepository.save(task);
    }

    public record Command(
            String title, String description, TaskStatus status, LocalDate dueDate) {}
}
