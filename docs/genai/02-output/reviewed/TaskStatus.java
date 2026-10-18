package com.example.taskmgmt.domain.task;

/**
 * Lifecycle status of a task. Invalid wire values must map to HTTP 400
 * (handled at the web layer via enum deserialization / Bean Validation).
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
