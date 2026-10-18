package com.example.taskmgmt.domain.task;

/**
 * Raised when an update's expected version does not match the aggregate version.
 * Mapped by the web layer to HTTP 409 with a stable problem type.
 */
public final class OptimisticLockConflictException extends RuntimeException {

    private final Long taskId;
    private final long expectedVersion;
    private final long actualVersion;

    public OptimisticLockConflictException(Long taskId, long expectedVersion, long actualVersion) {
        super("Task version mismatch");
        this.taskId = taskId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public Long getTaskId() {
        return taskId;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public long getActualVersion() {
        return actualVersion;
    }
}
