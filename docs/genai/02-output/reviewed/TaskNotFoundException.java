package com.example.taskmgmt.domain.task;

/**
 * Task missing for the authenticated owner (includes "exists but owned by another user").
 * Mapped to HTTP 404 — never 403 — so existence is not confirmed across users.
 */
public final class TaskNotFoundException extends RuntimeException {

    private final Long taskId;

    public TaskNotFoundException(Long taskId) {
        super("Task not found");
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }
}
