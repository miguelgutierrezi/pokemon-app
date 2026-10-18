package com.example.taskmgmt.web.task;

import com.example.taskmgmt.domain.task.TaskStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Full update; version required for optimistic locking.
 *
 * <p>No {@code @FutureOrPresent} on {@code dueDate}: an overdue task must remain
 * updatable (e.g. mark DONE) when the date is unchanged. Domain enforces
 * future/present only when the due date <em>changes</em>.
 */
@Schema(name = "UpdateTaskRequest")
public record UpdateTaskRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull
        TaskStatus status,

        @JsonProperty("due_date")
        @Schema(type = "string", format = "date", nullable = true)
        LocalDate dueDate,

        @NotNull
        @Schema(description = "Current aggregate version; mismatch yields 409")
        Long version) {}
