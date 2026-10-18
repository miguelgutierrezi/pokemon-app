package com.example.taskmgmt.web.task;

import com.example.taskmgmt.application.task.CreateTaskUseCase;
import com.example.taskmgmt.application.task.DeleteTaskUseCase;
import com.example.taskmgmt.application.task.GetTaskUseCase;
import com.example.taskmgmt.application.task.ListTasksUseCase;
import com.example.taskmgmt.application.task.TaskRepository;
import com.example.taskmgmt.application.task.UpdateTaskUseCase;
import com.example.taskmgmt.domain.task.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter. Authenticated principal exposes a stable {@code userId} (Long).
 * {@code AuthenticatedUser} is assumed to already exist in the security module.
 */
@RestController
@RequestMapping(path = "/api/v1/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Tasks")
@SecurityRequirement(name = "bearer-jwt")
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final GetTaskUseCase getTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;

    public TaskController(
            CreateTaskUseCase createTaskUseCase,
            UpdateTaskUseCase updateTaskUseCase,
            GetTaskUseCase getTaskUseCase,
            ListTasksUseCase listTasksUseCase,
            DeleteTaskUseCase deleteTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.getTaskUseCase = getTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a task")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public TaskResponse create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateTaskRequest request) {
        Task created = createTaskUseCase.execute(
                principal.userId(),
                new CreateTaskUseCase.Command(
                        request.title(),
                        request.description(),
                        request.status(),
                        request.dueDate()));
        return TaskResponse.from(created);
    }

    @GetMapping
    @Operation(summary = "List own tasks (paginated)")
    @ApiResponse(responseCode = "200", description = "Page of tasks")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination (e.g. size > 50)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public TaskPageResponse list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(ListTasksUseCase.MAX_PAGE_SIZE) int size) {
        TaskRepository.PageResult<Task> result =
                listTasksUseCase.execute(principal.userId(), page, size);
        List<TaskResponse> content = result.content().stream().map(TaskResponse::from).toList();
        return new TaskPageResponse(content, result.page(), result.size(), result.totalElements());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by id")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(
            responseCode = "404",
            description = "Not found (missing or not owned)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public TaskResponse get(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        return TaskResponse.from(getTaskUseCase.execute(principal.userId(), id));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a task (optimistic lock via version)")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(
            responseCode = "404",
            description = "Not found (missing or not owned)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Version conflict",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public TaskResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        Task updated = updateTaskUseCase.execute(
                principal.userId(),
                id,
                new UpdateTaskUseCase.Command(
                        request.title(),
                        request.description(),
                        request.status(),
                        request.dueDate(),
                        request.version()));
        return TaskResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(
            responseCode = "404",
            description = "Not found (missing or not owned)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        deleteTaskUseCase.execute(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    /** Minimal stand-in for the existing security principal. */
    public record AuthenticatedUser(Long userId) {}

    public record TaskPageResponse(
            List<TaskResponse> content, int page, int size, long totalElements) {}
}
