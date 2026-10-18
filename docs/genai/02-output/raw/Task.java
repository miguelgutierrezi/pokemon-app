package com.example.taskmgmt.domain.task;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Task aggregate root. Domain module: no Spring / JPA dependencies.
 *
 * <p>Invariants:
 * <ul>
 *   <li>title is required, non-blank, max {@value #TITLE_MAX}</li>
 *   <li>description optional, max {@value #DESCRIPTION_MAX}</li>
 *   <li>status is never null</li>
 *   <li>dueDate, when present, must be {@code >= today} (UTC calendar date)</li>
 *   <li>version is non-negative; used for optimistic locking</li>
 * </ul>
 */
public final class Task {

    public static final int TITLE_MAX = 200;
    public static final int DESCRIPTION_MAX = 2000;

    private final Long id;
    private final Long ownerUserId;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDate dueDate;
    private long version;

    private Task(
            Long id,
            Long ownerUserId,
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            long version) {
        this.id = id;
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId");
        this.title = normalizeAndValidateTitle(title);
        this.description = normalizeAndValidateDescription(description);
        this.status = Objects.requireNonNull(status, "status");
        this.dueDate = validateDueDate(dueDate, LocalDate.now());
        if (version < 0) {
            throw new IllegalArgumentException("version must be non-negative");
        }
        this.version = version;
    }

    public static Task create(
            Long ownerUserId,
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate) {
        return new Task(
                null,
                ownerUserId,
                title,
                description,
                status != null ? status : TaskStatus.TODO,
                dueDate,
                0L);
    }

    /** Rehydrate from persistence (invariants still enforced). */
    public static Task reconstitute(
            Long id,
            Long ownerUserId,
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            long version) {
        Objects.requireNonNull(id, "id");
        return new Task(id, ownerUserId, title, description, status, dueDate, version);
    }

    public void update(
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            long expectedVersion) {
        if (this.version != expectedVersion) {
            throw new OptimisticLockConflictException(this.id, expectedVersion, this.version);
        }
        this.title = normalizeAndValidateTitle(title);
        this.description = normalizeAndValidateDescription(description);
        this.status = Objects.requireNonNull(status, "status");
        this.dueDate = validateDueDate(dueDate, LocalDate.now());
        this.version = this.version + 1;
    }

    private static String normalizeAndValidateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        String trimmed = title.trim();
        if (trimmed.length() > TITLE_MAX) {
            throw new IllegalArgumentException("title must be at most " + TITLE_MAX + " characters");
        }
        return trimmed;
    }

    private static String normalizeAndValidateDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() > DESCRIPTION_MAX) {
            throw new IllegalArgumentException(
                    "description must be at most " + DESCRIPTION_MAX + " characters");
        }
        return trimmed;
    }

    /**
     * Due-date policy: when provided, must be today or a future date (UTC local date).
     * Past dates are rejected.
     */
    static LocalDate validateDueDate(LocalDate dueDate, LocalDate today) {
        if (dueDate == null) {
            return null;
        }
        if (dueDate.isBefore(today)) {
            throw new IllegalArgumentException("dueDate must be today or in the future");
        }
        return dueDate;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getVersion() {
        return version;
    }
}
