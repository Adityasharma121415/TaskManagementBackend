package com.cars24.taskmanagement.backend.sla;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionRepository;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import com.cars24.taskmanagement.backend.service.impl.TaskExecutionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskExecutionServiceImplTest {

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    @Mock
    private TaskExecutionTimeRepository taskExecutionTimeRepository;

    @InjectMocks
    private TaskExecutionServiceImpl taskExecutionService;

    @Test
    public void testFindAll() {
        List<TaskExecutionEntity> dummyList = new ArrayList<>();
        TaskExecutionEntity entity = new TaskExecutionEntity();
        // Set minimal properties on entity if needed.
        dummyList.add(entity);
        when(taskExecutionRepository.findAll()).thenReturn(dummyList);

        List<TaskExecutionEntity> result = taskExecutionService.findAll();
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskExecutionRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Found() {
        TaskExecutionEntity entity = new TaskExecutionEntity();
        entity.setId("task1");
        when(taskExecutionRepository.findById("task1")).thenReturn(Optional.of(entity));

        Optional<TaskExecutionEntity> result = taskExecutionService.findById("task1");
        assertTrue(result.isPresent());
        assertEquals("task1", result.get().getId());
        verify(taskExecutionRepository, times(1)).findById("task1");
    }

    @Test
    public void testFindById_NotFound() {
        when(taskExecutionRepository.findById("task1")).thenReturn(Optional.empty());

        Optional<TaskExecutionEntity> result = taskExecutionService.findById("task1");
        assertFalse(result.isPresent());
        verify(taskExecutionRepository, times(1)).findById("task1");
    }

    @Test
    public void testSave() {
        TaskExecutionEntity task = new TaskExecutionEntity();
        task.setId("task1");
        when(taskExecutionRepository.save(any(TaskExecutionEntity.class))).thenReturn(task);

        TaskExecutionEntity savedTask = taskExecutionService.save(task);
        assertNotNull(savedTask);
        assertEquals("task1", savedTask.getId());
        verify(taskExecutionRepository, times(1)).save(task);
    }

    @Test
    public void testUpdateTaskExecutionTime_CreateNewEntity() {
        // Scenario: No existing TaskExecutionTimeEntity, so a new one is created.
        String taskId = "task1";
        String status = "COMPLETED";
        Instant createdAt = Instant.now();
        Instant updatedAt = createdAt.plusSeconds(5);
        String funnel = "sourcing";
        String applicationId = "app1";
        String entityId = "ent1";
        String channel = "channel1";

        // Simulate no existing entity.
        when(taskExecutionTimeRepository.findByApplicationIdAndEntityId(applicationId, entityId))
                .thenReturn(Optional.empty());
        // When saving a new entity, simply return the passed argument.
        when(taskExecutionTimeRepository.save(any(TaskExecutionTimeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Call the update method.
        taskExecutionService.updateTaskExecutionTime(taskId, status, createdAt, updatedAt, funnel, applicationId, entityId, channel);

        // Assert: Verify the repository methods were called.
        verify(taskExecutionTimeRepository, times(1)).findByApplicationIdAndEntityId(applicationId, entityId);
        // The method calls save twice: once when creating a new entity and once at the end.
        verify(taskExecutionTimeRepository, times(2)).save(any(TaskExecutionTimeEntity.class));
    }

    @Test
    public void testUpdateTaskExecutionTime_UpdateExistingEntity_NewSubtask() {
        // Scenario: An existing TaskExecutionTimeEntity exists but does not have the given subtask.
        String taskId = "task2";
        String status = "COMPLETED";
        Instant createdAt = Instant.now();
        Instant updatedAt = createdAt.plusSeconds(10);
        String funnel = "credit";
        String applicationId = "app2";
        String entityId = "ent2";
        String channel = "channel2";

        TaskExecutionTimeEntity existingEntity = new TaskExecutionTimeEntity();
        existingEntity.setApplicationId(applicationId);
        existingEntity.setEntityId(entityId);
        existingEntity.setChannel("oldChannel");
        // Initialize the list for "credit" funnel.
        existingEntity.setCredit(new ArrayList<>());

        when(taskExecutionTimeRepository.findByApplicationIdAndEntityId(applicationId, entityId))
                .thenReturn(Optional.of(existingEntity));
        when(taskExecutionTimeRepository.save(any(TaskExecutionTimeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Update the execution time.
        taskExecutionService.updateTaskExecutionTime(taskId, status, createdAt, updatedAt, funnel, applicationId, entityId, channel);

        // Assert: Check that the channel was updated.
        assertEquals(channel, existingEntity.getChannel());
        // Verify a new subtask was added to the "credit" funnel.
        assertNotNull(existingEntity.getCredit());
        assertFalse(existingEntity.getCredit().isEmpty());
        SubTaskEntity subTask = existingEntity.getCredit().stream()
                .filter(st -> st.getTaskId().equals(taskId))
                .findFirst()
                .orElse(null);
        assertNotNull(subTask);
        // For status "COMPLETED", a new subtask should have duration = updatedAt - createdAt.
        long expectedDuration = updatedAt.toEpochMilli() - createdAt.toEpochMilli();
        assertEquals(expectedDuration, subTask.getDuration());
        // The sendbacks value should have incremented from -1 to 0.
        assertEquals(0, subTask.getSendbacks());
        // Verify that the subtask's updatedAt was set.
        assertEquals(updatedAt, subTask.getUpdatedAt());
        verify(taskExecutionTimeRepository, times(1)).findByApplicationIdAndEntityId(applicationId, entityId);
        verify(taskExecutionTimeRepository, atLeastOnce()).save(existingEntity);
    }

    @Test
    public void testUpdateTaskExecutionTime_UpdateExistingEntity_UpdateExistingSubtask() {
        // Scenario: An existing TaskExecutionTimeEntity exists and already has a subtask for the given taskId.
        String taskId = "task3";
        String status = "COMPLETED";
        Instant createdAt = Instant.now();
        Instant firstUpdatedAt = createdAt.plusSeconds(5);
        Instant secondUpdatedAt = createdAt.plusSeconds(15);
        String funnel = "conversion";
        String applicationId = "app3";
        String entityId = "ent3";
        String channel = "channel3";

        TaskExecutionTimeEntity existingEntity = new TaskExecutionTimeEntity();
        existingEntity.setApplicationId(applicationId);
        existingEntity.setEntityId(entityId);
        existingEntity.setChannel("oldChannel");

        // For "conversion", add an existing subtask.
        ArrayList<SubTaskEntity> conversionList = new ArrayList<>();
        SubTaskEntity existingSubTask = new SubTaskEntity(taskId, createdAt);
        // Simulate an initial update (which sets duration = firstUpdatedAt - createdAt and sendbacks from -1 to 0).
        existingSubTask.updateStatus("COMPLETED", firstUpdatedAt);
        conversionList.add(existingSubTask);
        existingEntity.setConversion(conversionList);

        when(taskExecutionTimeRepository.findByApplicationIdAndEntityId(applicationId, entityId))
                .thenReturn(Optional.of(existingEntity));
        when(taskExecutionTimeRepository.save(any(TaskExecutionTimeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Update the same subtask a second time.
        taskExecutionService.updateTaskExecutionTime(taskId, status, createdAt, secondUpdatedAt, funnel, applicationId, entityId, channel);

        // Assert: Verify the channel update.
        assertEquals(channel, existingEntity.getChannel());
        // The subtask should be present.
        assertNotNull(existingEntity.getConversion());
        assertFalse(existingEntity.getConversion().isEmpty());
        SubTaskEntity subTask = existingEntity.getConversion().stream()
                .filter(st -> st.getTaskId().equals(taskId))
                .findFirst()
                .orElse(null);
        assertNotNull(subTask);
        // The subtask's duration now is the sum of:
        // (firstUpdatedAt - createdAt) from the first update and (secondUpdatedAt - createdAt) from the second update.
        long expectedDuration = (firstUpdatedAt.toEpochMilli() - createdAt.toEpochMilli())
                + (secondUpdatedAt.toEpochMilli() - createdAt.toEpochMilli());
        assertEquals(expectedDuration, subTask.getDuration());
        // sendbacks should have incremented from -1 to 0 on the first call and then to 1 after the second update.
        assertEquals(1, subTask.getSendbacks());
        // The updatedAt field should reflect the latest update.
        assertEquals(secondUpdatedAt, subTask.getUpdatedAt());
        verify(taskExecutionTimeRepository, times(1)).findByApplicationIdAndEntityId(applicationId, entityId);
        verify(taskExecutionTimeRepository, atLeastOnce()).save(existingEntity);
    }
}
