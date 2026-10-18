package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskNotFoundException;
import java.util.Objects;

public final class DeleteTaskUseCase {

    private final TaskRepository taskRepository;

    public DeleteTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    public void execute(Long ownerUserId, Long taskId) {
        Task task = taskRepository
                .findByIdAndOwnerUserId(taskId, ownerUserId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        taskRepository.delete(task);
    }
}
