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
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActorServiceImplTest {

    @Mock
    private ActorDaoImpl actorDao;

    @InjectMocks
    private ActorServiceImpl actorService;

    private ActorEntity actorEntity;
    private TaskEntity validTask;
    private TaskEntity invalidTask;
    private TaskEntity task1, task2, task3;

    private List<ActorEntity> mockActorDocuments;
    private List<ActorEntity> mockAllDocuments;
    private List<ActorEntity> mockSystemDocuments;

    @BeforeEach
    void setup() {
        validTask = TaskEntity.builder()
                .taskId("dob_check")
                .status("IN_PROGRESS")
                .build();
        invalidTask = TaskEntity.builder()
                .taskId("ogl_check")
                .status("COMPLETED")
                .build();

        task1 = TaskEntity.builder().taskId("task_1").visited(3).duration(120.0).status("COMPLETED").build();
        task2 = TaskEntity.builder().taskId("task_2").visited(5).duration(130.0).status("TODO").build();
        task3 = TaskEntity.builder().taskId("task_3").visited(1).duration(200.0).status("COMPLETED").build();

        List<TaskEntity> tasks = Arrays.asList(validTask, invalidTask);

        actorEntity = ActorEntity.builder()
                .actorId("12345")
                .tasks(tasks)
                .applicationId("UCE1000003490")
                .build();


        mockActorDocuments = Arrays.asList(actorEntity);

        mockAllDocuments = Arrays.asList(
                ActorEntity.builder()
                        .actorId("99999")
                        .tasks(Arrays.asList(
                                TaskEntity.builder().taskId("task_1").duration(100.0).build(),
                                TaskEntity.builder().taskId("task_1").duration(140.0).build(),
                                TaskEntity.builder().taskId("task_2").duration(220.0).build()
                        ))
                        .build()
        );

        mockSystemDocuments = Arrays.asList(
            ActorEntity.builder()
                .tasks(Arrays.asList(
                    TaskEntity.builder().taskId("task_3").duration(2.5).build(),
                    TaskEntity.builder().taskId("task_4").duration(4.5).build()
                ))
                .build()
        );

        setFieldValue(actorService, "actorDocuments", mockActorDocuments);
        setFieldValue(actorService, "systemDocuments", mockSystemDocuments);
        setFieldValue(actorService, "allDocuments", mockAllDocuments);

    }

    @Test
    void testGetApplications() {
        String actorId = "12345";
        int days = 7;
        Date pastDate = new Date();

        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class)))
                .thenReturn(mockActorDocuments);

        actorService.getApplications(actorId, days);

        verify(actorDao, times(1)).findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class));

        List<ActorEntity> actualDocuments = getPrivateField(actorService, "actorDocuments");
        assertEquals(1, actualDocuments.size());
        assertEquals("12345", actualDocuments.get(0).getActorId());
        assertEquals("UCE1000003490", actualDocuments.get(0).getApplicationId());
    }

    @Test
    void testGetAllApplications() {
        String actorType = "SOURCING";
        int days = 30;

        when(actorDao.findAllApplications(eq(actorType), any(Date.class)))
                .thenReturn(mockAllDocuments);

        actorService.getAllApplications(actorType, days);

        verify(actorDao, times(1)).findAllApplications(eq(actorType), any(Date.class));

        List<ActorEntity> actualDocuments = getPrivateField(actorService, "allDocuments");
        assertEquals(1, actualDocuments.size());
        assertEquals("99999", actualDocuments.get(0).getActorId());
    }

    @Test
    void testGetTaskEfficiencyScoreNoTasks() {
        setFieldValue(actorService, "actorDocuments", new ArrayList<>());

        double efficiencyScore = actorService.getTaskEffiencyScore("12345");

        assertEquals(0.0, efficiencyScore);
    }

    @Test
    void testGetTaskEfficiencyScoreValidData() {
        double efficiencyScore = actorService.getTaskEffiencyScore("12345");

        assertTrue(efficiencyScore >= 0.0 && efficiencyScore <= 100.0);
    }

    private <T> T getPrivateField(Object targetObject, String fieldName) {
        try {
            Field field = targetObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(targetObject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetTasksAssignedWithNumericId() {
        List<Map<String, String>> result = actorService.getTasksAssigned("12345");

        assertEquals(1, result.size());
        assertEquals("dob_check", result.get(0).get("task_name"));
        assertEquals("UCE1000003490", result.get(0).get("application_id"));
        assertEquals("IN_PROGRESS", result.get(0).get("status"));
    }

    private void setFieldValue(Object targetObject, String fieldName, Object fieldValue) {
        try {
            Field field = targetObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(targetObject, fieldValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetTaskDurationValidActorId() {
        Map<String, Double[]> result = actorService.getTaskDuration("12345");

        assertNotNull(result);
        assertTrue(result.containsKey("dob_check"));

        Double[] durations = result.get("dob_check");
        assertEquals(4, durations.length);
        assertNotNull(durations[0]);
        assertNotNull(durations[1]);
        assertNotNull(durations[2]);
        assertNotNull(durations[3]);
    }

    @Test
    void testGetTasksSortedByRetriesValidNumericActorId() {
        String actorId = "12345";

        ActorEntity actor = ActorEntity.builder().actorId(actorId).tasks(Arrays.asList(task1, task2, task3)).build();
        setFieldValue(actorService, "actorDocuments", Collections.singletonList(actor));

        List<Map<String, Object>> result = actorService.getTasksSortedByRetries(actorId);

        assertEquals(3, result.size());
        assertEquals("task_2", result.get(0).get("task_id"));
        assertEquals(4, result.get(0).get("visited"));
    }

    @Test
    void testGetTasksSortedByRetriesValidFunnelId() {
        String funnelId = "SOURCING";

        ActorEntity systemActor = ActorEntity.builder().funnel(funnelId).tasks(Arrays.asList(task1, task2)).build();
        setFieldValue(actorService, "systemDocuments", Collections.singletonList(systemActor));

        List<Map<String, Object>> result = actorService.getTasksSortedByRetries(funnelId);

        assertEquals(2, result.size());
        assertEquals("task_2", result.get(0).get("task_id"));
        assertEquals(4, result.get(0).get("visited"));
    }

    @Test
    void testGetActorEmailWithHandledBy() {
        ActorEntity actorWithEmail = ActorEntity.builder().handledBy("raki@cars24.com").build();
        setFieldValue(actorService, "actorDocuments", Collections.singletonList(actorWithEmail));

        String result = actorService.getActorEmail();
        assertEquals("raki@cars24.com", result);
    }

    @Test
    void testGetActorTypeValidActorId() {
        String actorId = "12345";
        ActorEntity actor = ActorEntity.builder().actorId(actorId).actorType("SOURCING").build();
        setFieldValue(actorService, "actorDocuments", Collections.singletonList(actor));

        String result = actorService.getActorType(actorId);
        assertEquals("SOURCING", result);
    }

    @Test
    void testGetActorMetricsInvalidActorId() {
        Map<String, Object> result = actorService.getActorMetrics("abc", 10);
        assertEquals("Actor ID should be a number greater than 0.", result.get("Error"));

        result = actorService.getActorMetrics("-1", 10);
        assertEquals("Actor ID should be a number greater than 0.", result.get("Error"));

        result = actorService.getActorMetrics(null, 10);
        assertEquals("Actor ID should be a number greater than 0.", result.get("Error"));
    }

    @Test
    void testGetActorMetricsInvalidDaysRange() {
        Map<String, Object> result = actorService.getActorMetrics("12345", 5);
        assertEquals("Days must be greater than 7 and less than 90.", result.get("Error"));

        result = actorService.getActorMetrics("12345", 100);
        assertEquals("Days must be greater than 7 and less than 90.", result.get("Error"));
    }

    @Test
    void testGetActorMetrics_NoActorData() {
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq("12345"), any(Date.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = actorService.getActorMetrics("12345", 10);
        assertEquals("Actor data unavailable.", result.get("Error"));
    }

    @Test
    void testGetSystemMetricsInvalidFunnel() {
        Map<String, Object> result = actorService.getSystemMetrics("SOURCING", 10);
        assertEquals("Funnel data unavailable.", result.get("Error"));

        result = actorService.getSystemMetrics("-1", 10);
        assertEquals("Funnel data unavailable.", result.get("Error"));

        result = actorService.getSystemMetrics(null, 10);
        assertEquals("Funnel should be a word.", result.get("Error"));
    }

    @Test
    void testGetSystemMetricsInvalidDaysRange() {
        Map<String, Object> result = actorService.getSystemMetrics("SOURCING", 5);
        assertEquals("Days must be greater than 7 and less than 90.", result.get("Error"));

        result = actorService.getSystemMetrics("SOURCING", 100);
        assertEquals("Days must be greater than 7 and less than 90.", result.get("Error"));
    }

    @Test
    void testGetSystemMetrics_NoActorData() {
        when(actorDao.findAllByFunnelAndLastUpdatedAtAfter(eq("SOURCING"), any(Date.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = actorService.getSystemMetrics("SOURCING", 10);
        assertEquals("Funnel data unavailable.", result.get("Error"));
    }

    @Test
    void testGetTasksCompletedActorId() {

        ActorEntity actor = ActorEntity.builder().actorId("12345").tasks(Arrays.asList(task1, task2, task3)).build();
        setFieldValue(actorService, "actorDocuments", Collections.singletonList(actor));

        int result = actorService.getTasksCompleted("12345");

        assertEquals(2, result);
    }

    @Test
    void testGetTasksCompletedFunnel() {

        ActorEntity actor = ActorEntity.builder().funnel("SOURCING").tasks(Arrays.asList(task1, task2, task3)).build();
        setFieldValue(actorService, "systemDocuments", Collections.singletonList(actor));

        int result = actorService.getTasksCompleted("SOURCING");

        assertEquals(2, result);
    }

    @Test
    void testGetScoreAgentP90LessThanOrEqualToGlobalP90() {
        double result = actorService.getScore(50, 50, 60, 70);
        assertEquals(1.0, result);
    }

    @Test
    void testGetScoreAgentP90GreaterThanGlobalP90ButLessThanOrEqualToGlobalP95() {
        double result = actorService.getScore(55, 50, 60, 70);
        assertEquals(0.75, result);
    }

    @Test
    void testGetScoreAgentP90GreaterThanGlobalP95ButLessThanOrEqualToGlobalP99() {
        double result = actorService.getScore(65, 50, 60, 70);
        assertEquals(0.5, result);
    }

    @Test
    void testGetScoreAgentP90GreaterThanGlobalP99() {
        double result = actorService.getScore(75, 50, 60, 70);
        assertEquals(0.25, result);
    }

    @Test
    void testTaskRetriesActor() {
        String actorId = "12345";

        Map<String, Integer> retriesMap = new HashMap<>();
        retriesMap.put("task_1", 4);
        retriesMap.put("task_2", 6);

        ActorServiceImpl spyActorService = Mockito.spy(actorService);
        Mockito.doReturn(retriesMap).when(spyActorService).retryFrequency(actorId);

        ActorEntity actor = ActorEntity.builder()
                .actorId(actorId)
                .tasks(Arrays.asList(
                        TaskEntity.builder().taskId("task_1").build(),
                        TaskEntity.builder().taskId("task_2").build()
                ))
                .build();
        setFieldValue(spyActorService, "actorDocuments", Collections.singletonList(actor));

        Map<String, Double> result = spyActorService.taskRetries(actorId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(4.0, result.get("task_1"));
        assertEquals(6.0, result.get("task_2"));
    }

    @Test
    void testTaskRetriesFunnel() {
        String funnelId = "SOURCING";

        Map<String, Integer> retriesMap = new HashMap<>();
        retriesMap.put("task_1", 3);
        retriesMap.put("task_3", 9);

        ActorServiceImpl spyActorService = Mockito.spy(actorService);
        Mockito.doReturn(retriesMap).when(spyActorService).retryFrequency(funnelId);

        ActorEntity systemActor = ActorEntity.builder()
                .funnel(funnelId)
                .tasks(Arrays.asList(
                        TaskEntity.builder().taskId("task_1").build(),
                        TaskEntity.builder().taskId("task_3").build()
                ))
                .build();
        setFieldValue(spyActorService, "systemDocuments", Collections.singletonList(systemActor));

        Map<String, Double> result = spyActorService.taskRetries(funnelId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(3.0, result.get("task_1"));
        assertEquals(9.0, result.get("task_3"));
    }

    @Test
    void testTaskRetriesThreshold() {
        ActorEntity actor1 = ActorEntity.builder()
                .actorId("123")
                .tasks(Arrays.asList(
                        TaskEntity.builder().taskId("task_1").build(),
                        TaskEntity.builder().taskId("task_2").build()
                ))
                .build();

        ActorEntity actor2 = ActorEntity.builder()
                .actorId("456")
                .tasks(Arrays.asList(
                        TaskEntity.builder().taskId("task_1").build(),
                        TaskEntity.builder().taskId("task_3").build()
                ))
                .build();

        setFieldValue(actorService, "allDocuments", Arrays.asList(actor1, actor2));

        Map<String, Integer> mockRetries = new HashMap<>();
        mockRetries.put("task_1", 4);
        mockRetries.put("task_2", 2);
        mockRetries.put("task_3", 3);

        ActorServiceImpl spyActorService = Mockito.spy(actorService);
        Mockito.doReturn(mockRetries).when(spyActorService).retryFrequencyThreshold();

        Map<String, Double> result = spyActorService.taskRetriesThreshold();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(2.0, result.get("task_1"));
        assertEquals(2.0, result.get("task_2"));
        assertEquals(3.0, result.get("task_3"));
    }

    @Test
    void testTaskFrequencyThreshold() {
        setFieldValue(actorService, "allDocuments", mockAllDocuments);

        Map<String, Integer> result = actorService.taskFrequencyThreshold();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(0, result.get("task_1"));
        assertEquals(0, result.get("task_2"));
    }

    @Test
    void testGetTaskTimeAcrossApplicationsActor() {
        setFieldValue(actorService, "actorDocuments", mockActorDocuments);

        Map<String, Double> result = actorService.getTaskTimeAcrossApplications("12345");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetTaskTimeAcrossApplicationsFunnel() {
        setFieldValue(actorService, "systemDocuments", mockSystemDocuments);

        Map<String, Double> result = actorService.getTaskTimeAcrossApplications("SOURCING");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2.5, result.get("task_3"));
        assertEquals(4.5, result.get("task_4"));
    }

    @Test
    void testThresholdTaskTimeAcrossApplications() {
        setFieldValue(actorService, "allDocuments", mockAllDocuments);

        Map<String, Double> result = actorService.thresholdTaskTimeAcrossApplications();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(240, result.get("task_1"));
        assertEquals(220.0, result.get("task_2"));
    }

    @Test
    void testRetryFrequencyActor() {
        setFieldValue(actorService, "actorDocuments", mockActorDocuments);

        Map<String, Integer> result = actorService.retryFrequency("12345");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testRetryFrequencyFunnel() {
        setFieldValue(actorService, "systemDocuments", mockSystemDocuments);

        Map<String, Integer> result = actorService.retryFrequency("SOURCING");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testRetryFrequencyThreshold() {
        setFieldValue(actorService, "allDocuments", mockAllDocuments);

        Map<String, Integer> result = actorService.retryFrequencyThreshold();

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}