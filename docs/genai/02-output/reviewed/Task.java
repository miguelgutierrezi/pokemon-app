package com.example.taskmgmt.domain.task;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Task aggregate root. Domain module: no Spring / JPA dependencies.
 *
 * <p>Optimistic locking is enforced by the persistence adapter via a compare-and-set
 * UPDATE (not by bumping {@code version} here before save).
 *
 * <p>Callers pass {@code today} from an injected {@link java.time.Clock} (UTC).
 * This type never calls {@code LocalDate.now()}.
 */
public final class Task {

    public static final int TITLE_MAX = 200;
    public static final int DESCRIPTION_MAX = 2000;

    private final Long id;
    private final Long ownerUserId;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final LocalDate dueDate;
    private final long version;

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
        this.dueDate = dueDate;
        if (version < 0) {
            throw new IllegalArgumentException("version must be non-negative");
        }
        this.version = version;
    }

    /** Create path: when {@code dueDate} is present, it must be {@code >= today}. */
    public static Task create(
            Long ownerUserId,
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            LocalDate today) {
        Objects.requireNonNull(today, "today");
        return new Task(
                null,
                ownerUserId,
                title,
                description,
                status != null ? status : TaskStatus.TODO,
                requireFutureOrPresent(dueDate, today),
                0L);
    }

    /**
     * Rehydrate from persistence. Does not re-apply the create-time due-date policy
     * (overdue tasks must remain loadable).
     */
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

    /**
     * Returns a new aggregate with updated fields; version unchanged until CAS succeeds.
     *
     * <p>An <em>unchanged</em> past {@code dueDate} is allowed (so an overdue task can be
     * marked {@code DONE}). A <em>changed</em> due date must be {@code >= today}.
     */
    public Task withUpdate(
            String title,
            String description,
            TaskStatus status,
            LocalDate dueDate,
            LocalDate today) {
        Objects.requireNonNull(today, "today");
        LocalDate nextDue = dueDate;
        if (!Objects.equals(this.dueDate, dueDate)) {
            nextDue = requireFutureOrPresent(dueDate, today);
        }
        return new Task(
                this.id,
                this.ownerUserId,
                title,
                description,
                status,
                nextDue,
                this.version);
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

    /** When provided, due date must be today or a future date (create / due-date change). */
    static LocalDate requireFutureOrPresent(LocalDate dueDate, LocalDate today) {
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
