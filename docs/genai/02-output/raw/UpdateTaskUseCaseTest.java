package com.example.taskmgmt.application.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskmgmt.domain.task.OptimisticLockConflictException;
import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskNotFoundException;
import com.example.taskmgmt.domain.task.TaskStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TDD-oriented tests: ownership (404, never leak cross-user existence)
 * and optimistic locking (409 via domain exception).
 */
@ExtendWith(MockitoExtension.class)
class UpdateTaskUseCaseTest {

    private static final Long OWNER_ID = 10L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long TASK_ID = 42L;

    @Mock
    private TaskRepository taskRepository;

    private UpdateTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTaskUseCase(taskRepository);
    }

    private static Task ownedTask(long version) {
        return Task.reconstitute(
                TASK_ID,
                OWNER_ID,
                "Original",
                null,
                TaskStatus.TODO,
                LocalDate.now().plusDays(1),
                version);
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("update of another user's task id returns not-found (not forbidden)")
        void foreignTaskLooksMissing() {
            // Repository port already scopes by owner — empty means 404 for foreign or missing
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OTHER_USER_ID))
                    .thenReturn(Optional.empty());

            TaskNotFoundException ex = assertThrows(
                    TaskNotFoundException.class,
                    () -> useCase.execute(
                            OTHER_USER_ID,
                            TASK_ID,
                            new UpdateTaskUseCase.Command(
                                    "Hijack",
                                    null,
                                    TaskStatus.DONE,
                                    LocalDate.now().plusDays(2),
                                    0L)));

            assertEquals(TASK_ID, ex.getTaskId());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("owner can update when version matches")
        void ownerUpdatesSuccessfully() {
            Task existing = ownedTask(0L);
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OWNER_ID))
                    .thenReturn(Optional.of(existing));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task updated = useCase.execute(
                    OWNER_ID,
                    TASK_ID,
                    new UpdateTaskUseCase.Command(
                            "Updated title",
                            "notes",
                            TaskStatus.IN_PROGRESS,
                            LocalDate.now().plusDays(3),
                            0L));

            assertEquals("Updated title", updated.getTitle());
            assertEquals(TaskStatus.IN_PROGRESS, updated.getStatus());
            assertEquals(1L, updated.getVersion());
            verify(taskRepository).save(eq(updated));
        }
    }

    @Nested
    @DisplayName("optimistic locking")
    class OptimisticLock {

        @Test
        @DisplayName("stale version raises OptimisticLockConflictException")
        void versionMismatch() {
            Task existing = ownedTask(2L);
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OWNER_ID))
                    .thenReturn(Optional.of(existing));

            OptimisticLockConflictException ex = assertThrows(
                    OptimisticLockConflictException.class,
                    () -> useCase.execute(
                            OWNER_ID,
                            TASK_ID,
                            new UpdateTaskUseCase.Command(
                                    "Stale write",
                                    null,
                                    TaskStatus.DONE,
                                    LocalDate.now().plusDays(1),
                                    0L)));

            assertEquals(TASK_ID, ex.getTaskId());
            assertEquals(0L, ex.getExpectedVersion());
            assertEquals(2L, ex.getActualVersion());
            verify(taskRepository, never()).save(any());
        }
    }
}
