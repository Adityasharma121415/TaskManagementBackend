package com.cars24.taskmanagement.backend.actor;

import com.cars24.taskmanagement.backend.data.dao.impl.ActorDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskEntity;
import com.cars24.taskmanagement.backend.service.impl.ActorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ActorServiceImplTest {

    @Mock
    private ActorDaoImpl actorDao;

    @Mock
    private ActorServiceImpl actorService;

    private List<ActorEntity> mockActorDocuments;
    private List<TaskEntity> mockTasks;

    @BeforeEach
    void setup() {
        TaskEntity task1 = TaskEntity.builder()
                .taskId("ogl_check")
                .duration(100.0)
                .visited(2)
                .status("COMPLETED")
                .build();
        TaskEntity task2 = TaskEntity.builder()
                .taskId("dob_check")
                .duration(200.0)
                .visited(3)
                .status("IN_PROGRESS")
                .build();

        mockTasks = Arrays.asList(task1, task2);

        ActorEntity actor = ActorEntity.builder()
                .actorId("12345")
                .tasks(mockTasks)
                .applicationId("UCE1000003490")
                .handledBy("rahul.sharma@cars24.com")
                .actorType("SOURCING")
                .build();

        mockActorDocuments = Collections.singletonList(actor);
    }

    @Test
    void testGetTasksAssigned() {
        List<Map<String, String>> mockResponse = Collections.singletonList(
                Map.of("task_id", "dob_check")
        );
        Mockito.when(actorService.getTasksAssigned("12345")).thenReturn(mockResponse);

        List<Map<String, String>> result = actorService.getTasksAssigned("12345");

        assertEquals(1, result.size());
        assertEquals("dob_check", result.get(0).get("task_id"));
    }

    @Test
    void testGetMostAndLeastRetriedTask() {
        Mockito.when(actorService.getMostAndLeastRetriedTask("12345"))
                .thenReturn(Map.of("most_retried_task", "ogl_check", "least_retried_task", "dob_check"));

        Map<String, Object> result = actorService.getMostAndLeastRetriedTask("12345");

        assertNotNull(result.get("most_retried_task"));
        assertNotNull(result.get("least_retried_task"));
    }

    @Test
    void testGetAverageTaskTime() {
        Mockito.when(actorService.getAverageTaskTime("12345"))
                .thenReturn(Map.of("ogl_check", 50.0, "dob_check", 75.0));

        Map<String, Double> result = actorService.getAverageTaskTime("12345");

        assertTrue(result.containsKey("ogl_check"));
        assertTrue(result.containsKey("dob_check"));
    }

    @Test
    void testTaskRetries() {
        Mockito.when(actorService.taskRetries("12345"))
                .thenReturn(Map.of("ogl_check", 2.0, "dob_check", 3.0));

        Map<String, Double> result = actorService.taskRetries("12345");

        assertTrue(result.containsKey("ogl_check"));
        assertTrue(result.containsKey("dob_check"));
    }

    @Test
    void testGetActorType() {
        Mockito.when(actorService.getActorType("12345")).thenReturn("SOURCING");

        String result = actorService.getActorType("12345");

        assertEquals("SOURCING", result);
    }

    @Test
    void testGetActorEmail() {
        Mockito.when(actorService.getActorEmail()).thenReturn("rahul.sharma@cars24.com");

        String result = actorService.getActorEmail();

        assertEquals("rahul.sharma@cars24.com", result);
    }

    @Test
    void testGetFastestAndSlowestTask() {
        Mockito.when(actorService.getFastestAndSlowestTask("12345"))
                .thenReturn(Map.of("fastest_task", "ogl_check", "slowest_task", "dob_check"));

        Map<String, Object> result = actorService.getFastestAndSlowestTask("12345");

        assertNotNull(result.get("fastest_task"));
        assertNotNull(result.get("slowest_task"));
    }

    @Test
    void testTaskRetriesThreshold() {
        Mockito.when(actorService.taskRetriesThreshold())
                .thenReturn(Map.of("ogl_check", 2.0));

        Map<String, Double> result = actorService.taskRetriesThreshold();

        assertTrue(result.containsKey("ogl_check"));
    }

    @Test
    void testThresholdAverageTaskTime() {
        Mockito.when(actorService.thresholdAverageTaskTime())
                .thenReturn(Map.of("ogl_check", 100.0));

        Map<String, Double> result = actorService.thresholdAverageTaskTime();

        assertTrue(result.containsKey("ogl_check"));
    }

    @Test
    void testGetTaskEfficiencyScore() {
        Mockito.when(actorService.getTaskEffiencyScore("12345")).thenReturn(95.5);

        double score = actorService.getTaskEffiencyScore("12345");

        assertTrue(score >= 0);
        assertEquals(95.5, score);
    }

    @Test
    void testGetTasksCompleted() {
        Mockito.when(actorService.getTasksCompleted("12345")).thenReturn(1);

        int completed = actorService.getTasksCompleted("12345");

        assertEquals(1, completed);
    }
}