package com.example.taskmgmt.application.task;

import com.example.taskmgmt.domain.task.Task;
import java.util.Objects;

public final class ListTasksUseCase {

    /** Hard max page size; larger requests are rejected at the web layer with 400. */
    public static final int MAX_PAGE_SIZE = 50;

    private final TaskRepository taskRepository;

    public ListTasksUseCase(TaskRepository taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    public TaskRepository.PageResult<Task> execute(Long ownerUserId, int page, int size) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return taskRepository.findByOwnerUserId(ownerUserId, page, size);
    }
}
