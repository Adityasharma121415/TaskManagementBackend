package com.cars24.taskmanagement.backend.sla;

import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SubTaskEntityTest {

    @Test
    public void testConstructorInitialization() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);

        assertEquals("task1", subTask.getTaskId());
        // new_time should be set to createdAt on construction.
        assertEquals(createdAt, subTask.getNew_time());
        // Default values
        assertEquals(0, subTask.getVisited());
        assertEquals(-1, subTask.getSendbacks());
    }

    @Test
    public void testUpdateStatusTodo() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);
        Instant todoTime = createdAt.plus(10, ChronoUnit.SECONDS);

        subTask.updateStatus("TODO", todoTime);

        assertEquals(todoTime, subTask.getTodo_time());
        // visited should increase by 1.
        assertEquals(1, subTask.getVisited());
        assertEquals(todoTime, subTask.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusCompletedWithTodoAndVisitedMoreThanOne() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);
        // First update: TODO call.
        Instant firstTodo = createdAt.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", firstTodo);
        // Second TODO to simulate a revisit (visited becomes 2).
        Instant secondTodo = firstTodo.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", secondTodo);

        Instant completedTime = secondTodo.plus(10, ChronoUnit.SECONDS);
        subTask.updateStatus("COMPLETED", completedTime);

        // For visited > 1, COMPLETED should use the latest todo_time.
        assertEquals(completedTime, subTask.getCompleted_time());
        long expectedDuration = completedTime.toEpochMilli() - secondTodo.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
        // sendbacks should increment from -1 to 0.
        assertEquals(0, subTask.getSendbacks());
        assertEquals(completedTime, subTask.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusCompletedWithNew() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);
        // Without any TODO call, todo_time remains null.
        Instant completedTime = createdAt.plus(15, ChronoUnit.SECONDS);
        subTask.updateStatus("COMPLETED", completedTime);

        assertEquals(completedTime, subTask.getCompleted_time());
        // Duration should be computed as completedTime - new_time (which is createdAt).
        long expectedDuration = completedTime.toEpochMilli() - createdAt.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
        // sendbacks should increment from -1 to 0.
        assertEquals(0, subTask.getSendbacks());
        assertEquals(completedTime, subTask.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusSendbackWithVisitedMoreThanOne() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);
        // Two TODO calls: visited becomes 2.
        Instant firstTodo = createdAt.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", firstTodo);
        Instant secondTodo = firstTodo.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", secondTodo);

        Instant sendbackTime = secondTodo.plus(10, ChronoUnit.SECONDS);
        subTask.updateStatus("SENDBACK", sendbackTime);

        // SENDBACK should always set sendback_time.
        assertEquals(sendbackTime, subTask.getSendback_time());
        // Since visited > 1, duration should increase by sendbackTime - latest todo_time.
        long expectedDuration = sendbackTime.toEpochMilli() - secondTodo.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
    }

    @Test
    public void testUpdateStatusSendbackWithVisitedOne() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);
        // Single TODO call: visited becomes 1.
        Instant todoTime = createdAt.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", todoTime);

        Instant sendbackTime = todoTime.plus(10, ChronoUnit.SECONDS);
        subTask.updateStatus("SENDBACK", sendbackTime);

        // SENDBACK should set sendback_time.
        assertEquals(sendbackTime, subTask.getSendback_time());
        // When visited == 1, the else-if branch is taken, so duration is sendbackTime - new_time.
        long expectedDuration = sendbackTime.toEpochMilli() - createdAt.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
    }

    @Test
    public void testUpdateStatusFailed() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);
        // Increase visited with a TODO update.
        Instant todoTime = createdAt.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", todoTime);
        assertEquals(1, subTask.getVisited());

        Instant failedTime = todoTime.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("FAILED", failedTime);

        // FAILED should decrement visited but not below 0.
        assertEquals(0, subTask.getVisited());
        assertEquals(failedTime, subTask.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusNew() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);
        Instant newTime = createdAt.plus(10, ChronoUnit.SECONDS);
        subTask.updateStatus("NEW", newTime);

        assertEquals(newTime, subTask.getNew_time());
        assertEquals(newTime, subTask.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusSkipped() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);
        Instant skippedTime = createdAt.plus(20, ChronoUnit.SECONDS);
        subTask.updateStatus("SKIPPED", skippedTime);

        // SKIPPED should increase duration by skippedTime - new_time.
        long expectedDuration = skippedTime.toEpochMilli() - createdAt.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
        assertEquals(skippedTime, subTask.getUpdatedAt());
    }
}
