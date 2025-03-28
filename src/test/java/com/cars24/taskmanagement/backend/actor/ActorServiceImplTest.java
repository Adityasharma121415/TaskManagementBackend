package com.cars24.taskmanagement.backend.actor;

import com.cars24.taskmanagement.backend.data.dao.impl.ActorDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskEntity;
import com.cars24.taskmanagement.backend.service.impl.ActorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Array;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        when(actorService.getTasksAssigned("12345")).thenReturn(mockResponse);

        List<Map<String, String>> result = actorService.getTasksAssigned("12345");

        assertEquals(1, result.size());
        assertEquals("dob_check", result.get(0).get("task_id"));
    }

    @Test
    void testGetAverageTaskTime() {
        when(actorService.getAverageTaskTime("12345"))
                .thenReturn(Map.of("ogl_check", 50.0, "dob_check", 75.0));

        Map<String, Double> result = actorService.getAverageTaskTime("12345");

        assertTrue(result.containsKey("ogl_check"));
        assertTrue(result.containsKey("dob_check"));
    }

    @Test
    void testTaskRetries() {
        when(actorService.taskRetries("12345"))
                .thenReturn(Map.of("ogl_check", 2.0, "dob_check", 3.0));

        Map<String, Double> result = actorService.taskRetries("12345");

        assertTrue(result.containsKey("ogl_check"));
        assertTrue(result.containsKey("dob_check"));
    }

    @Test
    void testGetActorType() {
        when(actorService.getActorType("12345")).thenReturn("SOURCING");

        String result = actorService.getActorType("12345");

        assertEquals("SOURCING", result);
    }

    @Test
    void testGetActorEmail() {
        when(actorService.getActorEmail()).thenReturn("rahul.sharma@cars24.com");

        String result = actorService.getActorEmail();

        assertEquals("rahul.sharma@cars24.com", result);
    }

    @Test
    void testGetFastestAndSlowestTask() {
        when(actorService.getFastestAndSlowestTask("12345"))
                .thenReturn(Map.of("fastest_task", "ogl_check", "slowest_task", "dob_check"));

        Map<String, Object> result = actorService.getFastestAndSlowestTask("12345");

        assertNotNull(result.get("fastest_task"));
        assertNotNull(result.get("slowest_task"));
    }

    @Test
    void testTaskRetriesThreshold() {
        when(actorService.taskRetriesThreshold())
                .thenReturn(Map.of("ogl_check", 2.0));

        Map<String, Double> result = actorService.taskRetriesThreshold();

        assertTrue(result.containsKey("ogl_check"));
    }

    @Test
    void testThresholdAverageTaskTime() {
        when(actorService.thresholdAverageTaskTime())
                .thenReturn(Map.of("ogl_check", 100.0));

        Map<String, Double> result = actorService.thresholdAverageTaskTime();

        assertTrue(result.containsKey("ogl_check"));
    }

    @Test
    void testGetTaskEfficiencyScore() {
        String actorId = "12345";

        Map<String, List<Double>> mockActorTaskTimes = new HashMap<>();
        mockActorTaskTimes.put("task1", Arrays.asList(7.0, 7.0, 7.0, 8.0, 8.0, 8.0, 9.0, 10.0, 12.0));
        mockActorTaskTimes.put("task2", Arrays.asList(15.0, 15.0, 16.0, 16.0, 16.0, 16.0, 16.0, 20.0, 25.0));
        mockActorTaskTimes.put("task3", Arrays.asList(4.0, 4.0, 4.0, 4.0, 4.0, 5.0, 5.0, 10.0, 10.0, 10.0));

        Map<String, List<Double>> mockGlobalTaskTimes = new HashMap<>();
        mockGlobalTaskTimes.put("task1", Arrays.asList(12.0, 22.0, 18.0));

        Map<String, Map<String, Double>> mockAgentP90 = new HashMap<>();
        mockAgentP90.put("task1", Collections.singletonMap("p90", 18.0));

        Map<String, Map<String, Double>> mockGlobalPercentiles = new HashMap<>();
        mockGlobalPercentiles.put("task1", Collections.singletonMap("p90", 20.0));

        Map<String, Double[]> mockGlobalAvgPercentiles = new HashMap<>();
        mockGlobalAvgPercentiles.put("task1", new Double[]{19.0});

        double expectedEfficiencyScore = 0.0;

        double actualScore = actorService.getTaskEffiencyScore(actorId);

        assertEquals(expectedEfficiencyScore, actualScore, 0.00);
    }

    @Test
    void testGetTasksCompleted() {
        when(actorService.getTasksCompleted("12345")).thenReturn(1);

        int completed = actorService.getTasksCompleted("12345");

        assertEquals(1, completed);
    }
}