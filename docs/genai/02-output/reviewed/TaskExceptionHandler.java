package com.example.taskmgmt.web.task;

import com.example.taskmgmt.domain.task.OptimisticLockConflictException;
import com.example.taskmgmt.domain.task.TaskNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RFC 7807 mappings. Never put exception class names or stack traces in the body.
 * Does <strong>not</strong> map bare {@link IllegalArgumentException} globally to 400.
 */
@RestControllerAdvice
public class TaskExceptionHandler {

    private static final URI TYPE_NOT_FOUND = URI.create("https://api.example.com/problems/not-found");
    private static final URI TYPE_CONFLICT =
            URI.create("https://api.example.com/problems/optimistic-lock");
    private static final URI TYPE_VALIDATION =
            URI.create("https://api.example.com/problems/validation");

    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleNotFound(TaskNotFoundException ignored) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Task not found");
        problem.setTitle("Not Found");
        problem.setType(TYPE_NOT_FOUND);
        return problem;
    }

    @ExceptionHandler(OptimisticLockConflictException.class)
    public ProblemDetail handleConflict(OptimisticLockConflictException ignored) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The task was modified by another request; reload and retry");
        problem.setTitle("Conflict");
        problem.setType(TYPE_CONFLICT);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Bad Request");
        problem.setType(TYPE_VALIDATION);
        problem.setProperty(
                "errors",
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .toList());
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ignored) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Bad Request");
        problem.setType(TYPE_VALIDATION);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ignored) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid or unreadable request body");
        problem.setTitle("Bad Request");
        problem.setType(TYPE_VALIDATION);
        return problem;
    }
}
