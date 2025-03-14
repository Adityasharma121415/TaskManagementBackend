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

        assertEquals(createdAt, subTask.getNew_time());

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

        assertEquals(1, subTask.getVisited());
        assertEquals(todoTime, subTask.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusCompletedWithTodoAndVisitedMoreThanOne() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);

        Instant firstTodo = createdAt.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", firstTodo);

        Instant secondTodo = firstTodo.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", secondTodo);

        Instant completedTime = secondTodo.plus(10, ChronoUnit.SECONDS);
        subTask.updateStatus("COMPLETED", completedTime);


        assertEquals(completedTime, subTask.getCompleted_time());
        long expectedDuration = completedTime.toEpochMilli() - secondTodo.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());

        assertEquals(0, subTask.getSendbacks());
        assertEquals(completedTime, subTask.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusCompletedWithNew() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);

        Instant completedTime = createdAt.plus(15, ChronoUnit.SECONDS);
        subTask.updateStatus("COMPLETED", completedTime);

        assertEquals(completedTime, subTask.getCompleted_time());

        long expectedDuration = completedTime.toEpochMilli() - createdAt.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());

        assertEquals(0, subTask.getSendbacks());
        assertEquals(completedTime, subTask.getUpdatedAt());
    }

    @Test
    public void testUpdateStatusSendbackWithVisitedMoreThanOne() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);

        Instant firstTodo = createdAt.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", firstTodo);
        Instant secondTodo = firstTodo.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", secondTodo);

        Instant sendbackTime = secondTodo.plus(10, ChronoUnit.SECONDS);
        subTask.updateStatus("SENDBACK", sendbackTime);


        assertEquals(sendbackTime, subTask.getSendback_time());

        long expectedDuration = sendbackTime.toEpochMilli() - secondTodo.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
    }

    @Test
    public void testUpdateStatusSendbackWithVisitedOne() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);

        Instant todoTime = createdAt.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", todoTime);

        Instant sendbackTime = todoTime.plus(10, ChronoUnit.SECONDS);
        subTask.updateStatus("SENDBACK", sendbackTime);


        assertEquals(sendbackTime, subTask.getSendback_time());

        long expectedDuration = sendbackTime.toEpochMilli() - createdAt.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
    }

    @Test
    public void testUpdateStatusFailed() {
        Instant createdAt = Instant.now();
        SubTaskEntity subTask = new SubTaskEntity("task1", createdAt);

        Instant todoTime = createdAt.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("TODO", todoTime);
        assertEquals(1, subTask.getVisited());

        Instant failedTime = todoTime.plus(5, ChronoUnit.SECONDS);
        subTask.updateStatus("FAILED", failedTime);


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


        long expectedDuration = skippedTime.toEpochMilli() - createdAt.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
        assertEquals(skippedTime, subTask.getUpdatedAt());
    }
}
