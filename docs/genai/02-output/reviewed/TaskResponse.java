package com.example.taskmgmt.web.task;

import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(name = "TaskResponse")
public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        @JsonProperty("due_date") LocalDate dueDate,
        Long version) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getVersion());
    }
}
