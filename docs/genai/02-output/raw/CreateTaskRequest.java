package com.example.taskmgmt.web.task;

import com.example.taskmgmt.domain.task.TaskStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(name = "CreateTaskRequest", description = "Payload to create a task for the authenticated user")
public record CreateTaskRequest(
        @NotBlank
        @Size(max = 200)
        @Schema(example = "Buy groceries", maxLength = 200)
        String title,

        @Size(max = 2000)
        @Schema(nullable = true, maxLength = 2000)
        String description,

        @NotNull
        @Schema(example = "TODO", allowableValues = {"TODO", "IN_PROGRESS", "DONE"})
        TaskStatus status,

        @FutureOrPresent
        @JsonProperty("due_date")
        @Schema(type = "string", format = "date", example = "2026-10-20", nullable = true)
        LocalDate dueDate) {}
