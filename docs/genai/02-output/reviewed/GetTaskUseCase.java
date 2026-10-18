package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskNotFoundException;
import java.util.Objects;

public final class GetTaskUseCase {

    private final TaskRepository taskRepository;

    public GetTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    public Task execute(Long ownerUserId, Long taskId) {
        return taskRepository
                .findByIdAndOwnerUserId(taskId, ownerUserId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
