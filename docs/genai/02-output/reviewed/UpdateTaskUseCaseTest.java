package com.example.taskmgmt.application.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskmgmt.domain.task.OptimisticLockConflictException;
import com.example.taskmgmt.domain.task.Task;
import com.example.taskmgmt.domain.task.TaskNotFoundException;
import com.example.taskmgmt.domain.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateTaskUseCaseTest {

    private static final Long OWNER_ID = 10L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long TASK_ID = 42L;
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);

    @Mock
    private TaskRepository taskRepository;

    private UpdateTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTaskUseCase(taskRepository, FIXED_CLOCK);
    }

    private static Task ownedTask(long version) {
        return Task.reconstitute(
                TASK_ID,
                OWNER_ID,
                "Original",
                null,
                TaskStatus.TODO,
                TODAY.plusDays(1),
                version);
    }

    private static Task overdueTask(long version) {
        return Task.reconstitute(
                TASK_ID,
                OWNER_ID,
                "Overdue",
                null,
                TaskStatus.TODO,
                TODAY.minusDays(3),
                version);
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("update of another user's task id returns not-found (not forbidden)")
        void foreignTaskLooksMissing() {
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OTHER_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> useCase.execute(
                            OTHER_USER_ID,
                            TASK_ID,
                            new UpdateTaskUseCase.Command(
                                    "Hijack",
                                    null,
                                    TaskStatus.DONE,
                                    TODAY.plusDays(2),
                                    0L)));

            verify(taskRepository, never())
                    .updateIfVersion(anyLong(), anyLong(), anyLong(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("optimistic locking")
    class OptimisticLock {

        @Test
        @DisplayName("stale version raises OptimisticLockConflictException before write")
        void versionMismatch() {
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OWNER_ID))
                    .thenReturn(Optional.of(ownedTask(2L)));

            assertThrows(
                    OptimisticLockConflictException.class,
                    () -> useCase.execute(
                            OWNER_ID,
                            TASK_ID,
                            new UpdateTaskUseCase.Command(
                                    "Stale write",
                                    null,
                                    TaskStatus.DONE,
                                    TODAY.plusDays(1),
                                    0L)));

            verify(taskRepository, never())
                    .updateIfVersion(anyLong(), anyLong(), anyLong(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("CAS miss after probe still yields conflict")
        void casMiss() {
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OWNER_ID))
                    .thenReturn(Optional.of(ownedTask(0L)));
            when(taskRepository.updateIfVersion(
                            eq(OWNER_ID),
                            eq(TASK_ID),
                            eq(0L),
                            any(),
                            any(),
                            any(),
                            any()))
                    .thenReturn(Optional.empty());

            assertThrows(
                    OptimisticLockConflictException.class,
                    () -> useCase.execute(
                            OWNER_ID,
                            TASK_ID,
                            new UpdateTaskUseCase.Command(
                                    "Updated",
                                    null,
                                    TaskStatus.IN_PROGRESS,
                                    TODAY.plusDays(1),
                                    0L)));
        }

        @Test
        @DisplayName("owner update succeeds when CAS returns the row")
        void ownerUpdatesSuccessfully() {
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OWNER_ID))
                    .thenReturn(Optional.of(ownedTask(0L)));
            Task saved = Task.reconstitute(
                    TASK_ID,
                    OWNER_ID,
                    "Updated title",
                    "notes",
                    TaskStatus.IN_PROGRESS,
                    TODAY.plusDays(3),
                    1L);
            when(taskRepository.updateIfVersion(
                            eq(OWNER_ID),
                            eq(TASK_ID),
                            eq(0L),
                            any(),
                            any(),
                            any(),
                            any()))
                    .thenReturn(Optional.of(saved));

            Task updated = useCase.execute(
                    OWNER_ID,
                    TASK_ID,
                    new UpdateTaskUseCase.Command(
                            "Updated title",
                            "notes",
                            TaskStatus.IN_PROGRESS,
                            TODAY.plusDays(3),
                            0L));

            assertEquals("Updated title", updated.getTitle());
            assertEquals(1L, updated.getVersion());
        }
    }

    @Nested
    @DisplayName("overdue due date")
    class OverdueDueDate {

        @Test
        @DisplayName("unchanged past dueDate allows marking DONE")
        void overdueTaskCanBeCompleted() {
            Task existing = overdueTask(0L);
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OWNER_ID))
                    .thenReturn(Optional.of(existing));
            Task saved = Task.reconstitute(
                    TASK_ID,
                    OWNER_ID,
                    "Overdue",
                    null,
                    TaskStatus.DONE,
                    TODAY.minusDays(3),
                    1L);
            when(taskRepository.updateIfVersion(
                            eq(OWNER_ID),
                            eq(TASK_ID),
                            eq(0L),
                            any(),
                            any(),
                            eq(TaskStatus.DONE),
                            eq(TODAY.minusDays(3))))
                    .thenReturn(Optional.of(saved));

            Task updated = useCase.execute(
                    OWNER_ID,
                    TASK_ID,
                    new UpdateTaskUseCase.Command(
                            "Overdue",
                            null,
                            TaskStatus.DONE,
                            TODAY.minusDays(3),
                            0L));

            assertEquals(TaskStatus.DONE, updated.getStatus());
            assertEquals(TODAY.minusDays(3), updated.getDueDate());
        }

        @Test
        @DisplayName("changing dueDate to a past day is rejected")
        void cannotChangeDueDateToPast() {
            when(taskRepository.findByIdAndOwnerUserId(TASK_ID, OWNER_ID))
                    .thenReturn(Optional.of(ownedTask(0L)));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> useCase.execute(
                            OWNER_ID,
                            TASK_ID,
                            new UpdateTaskUseCase.Command(
                                    "Original",
                                    null,
                                    TaskStatus.TODO,
                                    TODAY.minusDays(1),
                                    0L)));

            verify(taskRepository, never())
                    .updateIfVersion(anyLong(), anyLong(), anyLong(), any(), any(), any(), any());
        }
    }
}
