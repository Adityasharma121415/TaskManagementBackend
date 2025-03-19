package com.cars24.taskmanagement.backend;

import com.cars24.taskmanagement.backend.data.dao.impl.ActorDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskEntity;
import com.cars24.taskmanagement.backend.service.impl.ActorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
        import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

        import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActorServiceImplTest {

    @Mock
    private ActorDaoImpl actorDao;

    @InjectMocks
    private ActorServiceImpl actorService;

    // Helper method to create an ActorEntity with tasks
    private ActorEntity createActor(String actorId, String actorType, String applicationId, List<TaskEntity> tasks) {
        ActorEntity actor = new ActorEntity();
        actor.setActorId(actorId);
        actor.setActorType(actorType);
        actor.setApplicationId(applicationId);
        actor.setTasks(tasks);
        return actor;
    }

    // Helper method to create a TaskEntity
    private TaskEntity createTask(String taskId, int visited, double duration, String status) {
        TaskEntity task = new TaskEntity();
        task.setTaskId(taskId);
        task.setVisited(visited);
        task.setDuration(duration);
        task.setStatus(status);
        return task;
    }

    @BeforeEach
    public void setUp() {
        // With @ExtendWith and @InjectMocks, mocks are automatically injected.
    }

    // 1. Test getApplications – indirectly by verifying a method using actorDocuments.
    @Test
    public void testGetApplications() {
        String actorId = "actor1";
        int days = 7;
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", new ArrayList<>());
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, days);
        // Since there are no tasks, tasks completed should be 0.
        int tasksCompleted = actorService.getTasksCompleted(actorId);
        assertEquals(0, tasksCompleted);
    }

    // 2. Test getAllApplications – indirectly by checking threshold methods using allDocuments.
    @Test
    public void testGetAllApplications() {
        String actorType = "TYPE1";
        int days = 7;
        ActorEntity actor = createActor("actor1", actorType, "app1", new ArrayList<>());
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllApplications(eq(actorType), any(Date.class))).thenReturn(list);

        actorService.getAllApplications(actorType, days);
        Map<String, Double> thresholdAvg = actorService.thresholdAverageTaskTime();
        // No tasks provided, so the returned map should be empty.
        assertTrue(thresholdAvg.isEmpty());
    }

    // 3. Test getTaskEffiencyScore
    @Test
    public void testGetTaskEffiencyScore() {
        String actorId = "actor1";
        int days = 7;
        // For actorDocuments: one task with low average time.
        TaskEntity actorTask = createTask("task1", 1, 10, "COMPLETED");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(actorTask));
        List<ActorEntity> actorList = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(actorList);

        // For allDocuments (threshold): the same task but with a higher average.
        TaskEntity thresholdTask = createTask("task1", 1, 20, "COMPLETED");
        ActorEntity thresholdActor = createActor("actor2", "TYPE1", "app2", Arrays.asList(thresholdTask));
        List<ActorEntity> thresholdList = Arrays.asList(thresholdActor);
        when(actorDao.findAllApplications(eq("TYPE1"), any(Date.class))).thenReturn(thresholdList);

        actorService.getApplications(actorId, days);
        actorService.getAllApplications("TYPE1", days);
        double efficiencyScore = actorService.getTaskEffiencyScore(actorId);
        // Since actor's average (10) is lower than threshold (20), score should be 100%
        assertEquals(100.0, efficiencyScore);
    }

    // 4. Test getFastestAndSlowestTask
    @Test
    public void testGetFastestAndSlowestTask() {
        String actorId = "actor1";
        TaskEntity fastTask = createTask("taskFast", 1, 5, "COMPLETED");
        TaskEntity slowTask = createTask("taskSlow", 1, 15, "COMPLETED");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(fastTask, slowTask));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        Map<String, Object> result = actorService.getFastestAndSlowestTask(actorId);
        assertNotNull(result);
        assertTrue(result.containsKey("fastest_task"));
        assertTrue(result.containsKey("slowest_task"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fastestTask = (Map<String, Object>) result.get("fastest_task");
        @SuppressWarnings("unchecked")
        Map<String, Object> slowestTask = (Map<String, Object>) result.get("slowest_task");

        assertEquals("taskFast", fastestTask.get("task_id"));
        assertEquals(5.0, fastestTask.get("duration"));
        assertEquals("taskSlow", slowestTask.get("task_id"));
        assertEquals(15.0, slowestTask.get("duration"));
    }

    // 5. Test getMostAndLeastRetriedTask
    @Test
    public void testGetMostAndLeastRetriedTask() {
        String actorId = "actor1";
        TaskEntity task1 = createTask("task1", 3, 10, "COMPLETED"); // effective retries = 2
        TaskEntity task2 = createTask("task2", 2, 20, "COMPLETED"); // effective retries = 1
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task1, task2));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        Map<String, Object> result = actorService.getMostAndLeastRetriedTask(actorId);
        assertNotNull(result);
        assertTrue(result.containsKey("most_retried_task"));
        assertTrue(result.containsKey("least_retried_task"));

        @SuppressWarnings("unchecked")
        Map<String, Object> mostRetriedTask = (Map<String, Object>) result.get("most_retried_task");
        @SuppressWarnings("unchecked")
        Map<String, Object> leastRetriedTask = (Map<String, Object>) result.get("least_retried_task");

        assertEquals("task1", mostRetriedTask.get("task_id"));
        // visited count is decremented by one in the result
        assertEquals(2, mostRetriedTask.get("visited"));
        assertEquals("task2", leastRetriedTask.get("task_id"));
        assertEquals(1, leastRetriedTask.get("visited"));
    }

    // 6. Test getPastDate
    @Test
    public void testGetPastDate() {
        int days = 5;
        Date past = actorService.getPastDate(days);
        long diffMillis = new Date().getTime() - past.getTime();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);
        // Allowing for small margin due to test execution time
        assertTrue(diffDays >= days);
    }

    // 7. Test taskFrequency
    @Test
    public void testTaskFrequency() {
        String actorId = "actor1";
        TaskEntity task1 = createTask("task1", 2, 10, "COMPLETED");
        TaskEntity task2 = createTask("task2", 3, 20, "NEW");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task1, task2));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        Map<String, Integer> frequency = actorService.taskFrequency(actorId);
        assertEquals(2, frequency.get("task1"));
        assertEquals(3, frequency.get("task2"));
    }

    // 8. Test retryFrequency
    @Test
    public void testRetryFrequency() {
        String actorId = "actor1";
        TaskEntity task1 = createTask("task1", 3, 10, "COMPLETED"); // effective retries = 2
        TaskEntity task2 = createTask("task2", 1, 20, "NEW");         // effective retries = 0
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task1, task2));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        Map<String, Integer> retryFreq = actorService.retryFrequency(actorId);
        assertEquals(2, retryFreq.get("task1"));
        assertEquals(0, retryFreq.get("task2"));
    }

    // 9. Test retryFrequencyThreshold (using allDocuments)
    @Test
    public void testRetryFrequencyThreshold() {
        TaskEntity task1 = createTask("task1", 4, 15, "COMPLETED"); // effective retries = 3
        ActorEntity actor = createActor("actorX", "TYPEX", "appX", Arrays.asList(task1));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllApplications(eq("TYPEX"), any(Date.class))).thenReturn(list);

        actorService.getAllApplications("TYPEX", 7);
        Map<String, Integer> retryFreqThreshold = actorService.retryFrequencyThreshold();
        assertEquals(3, retryFreqThreshold.get("task1"));
    }

    // 10. Test taskRetries
    @Test
    public void testTaskRetries() {
        String actorId = "actor1";
        // One occurrence of task1 with visited 3 gives retries 2 and frequency 1.
        TaskEntity task = createTask("task1", 3, 20, "COMPLETED");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        Map<String, Double> taskRetries = actorService.taskRetries(actorId);
        assertEquals(2.0, taskRetries.get("task1"));
    }

    // 11. Test taskRetriesThreshold
    @Test
    public void testTaskRetriesThreshold() {
        // One occurrence of task1 with visited 5 gives retries 4 and frequency 1.
        TaskEntity task = createTask("task1", 5, 30, "COMPLETED");
        ActorEntity actor = createActor("actorX", "TYPEX", "appX", Arrays.asList(task));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllApplications(eq("TYPEX"), any(Date.class))).thenReturn(list);

        actorService.getAllApplications("TYPEX", 7);
        Map<String, Double> taskRetriesThreshold = actorService.taskRetriesThreshold();
        assertEquals(4.0, taskRetriesThreshold.get("task1"));
    }

    // 12. Test taskFrequencyThreshold
    @Test
    public void testTaskFrequencyThreshold() {
        TaskEntity task1 = createTask("task1", 2, 10, "COMPLETED");
        TaskEntity task2 = createTask("task2", 3, 20, "NEW");
        ActorEntity actor = createActor("actorX", "TYPEX", "appX", Arrays.asList(task1, task2));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllApplications(eq("TYPEX"), any(Date.class))).thenReturn(list);

        actorService.getAllApplications("TYPEX", 7);
        Map<String, Integer> taskFreqThreshold = actorService.taskFrequencyThreshold();
        assertEquals(2, taskFreqThreshold.get("task1"));
        assertEquals(3, taskFreqThreshold.get("task2"));
    }

    // 13. Test getTaskTimeAcrossApplications
    @Test
    public void testGetTaskTimeAcrossApplications() {
        String actorId = "actor1";
        TaskEntity task1 = createTask("task1", 1, 10, "COMPLETED");
        TaskEntity task2 = createTask("task2", 1, 20, "NEW");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task1, task2));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        Map<String, Double> taskTimeMap = actorService.getTaskTimeAcrossApplications(actorId);
        assertEquals(10.0, taskTimeMap.get("task1"));
        assertEquals(20.0, taskTimeMap.get("task2"));
    }

    // 14. Test thresholdTaskTimeAcrossApplications
    @Test
    public void testThresholdTaskTimeAcrossApplications() {
        TaskEntity task1 = createTask("task1", 1, 15, "COMPLETED");
        ActorEntity actor = createActor("actorX", "TYPEX", "appX", Arrays.asList(task1));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllApplications(eq("TYPEX"), any(Date.class))).thenReturn(list);

        actorService.getAllApplications("TYPEX", 7);
        Map<String, Double> thresholdTaskTimeMap = actorService.thresholdTaskTimeAcrossApplications();
        assertEquals(15.0, thresholdTaskTimeMap.get("task1"));
    }

    // 15. Test getTasksCompleted
    @Test
    public void testGetTasksCompleted() {
        String actorId = "actor1";
        TaskEntity task1 = createTask("task1", 1, 10, "COMPLETED");
        TaskEntity task2 = createTask("task2", 1, 20, "NEW");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task1, task2));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        int completed = actorService.getTasksCompleted(actorId);
        assertEquals(1, completed);
    }

    // 16. Test getTasksAssigned
    @Test
    public void testGetTasksAssigned() {
        String actorId = "actor1";
        TaskEntity task1 = createTask("task1", 1, 10, "NEW");
        TaskEntity task2 = createTask("task2", 1, 20, "COMPLETED"); // Not assigned
        TaskEntity task3 = createTask("task3", 1, 30, "IN_PROGRESS");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task1, task2, task3));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        List<Map<String, String>> tasksAssigned = actorService.getTasksAssigned(actorId);
        // Only tasks with status NEW, IN_PROGRESS, or TODO are added.
        assertEquals(2, tasksAssigned.size());
    }

    // 17. Test getAverageTaskTime
    @Test
    public void testGetAverageTaskTime() {
        String actorId = "actor1";
        // Create a task that appears once with visited = 2 and total duration 20 => average 10.
        TaskEntity task1 = createTask("task1", 2, 20, "COMPLETED");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task1));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        Map<String, Double> averageTaskTime = actorService.getAverageTaskTime(actorId);
        assertEquals(10.0, averageTaskTime.get("task1"));
    }

    // 18. Test thresholdAverageTaskTime
    @Test
    public void testThresholdAverageTaskTime() {
        // For threshold: one task with visited = 2 and duration 30 gives average 15.
        TaskEntity task1 = createTask("task1", 2, 30, "COMPLETED");
        ActorEntity actor = createActor("actorX", "TYPEX", "appX", Arrays.asList(task1));
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllApplications(eq("TYPEX"), any(Date.class))).thenReturn(list);

        actorService.getAllApplications("TYPEX", 7);
        Map<String, Double> thresholdAverageTaskTime = actorService.thresholdAverageTaskTime();
        assertEquals(15.0, thresholdAverageTaskTime.get("task1"));
    }

    // 19. Test getActorType
    @Test
    public void testGetActorType() {
        String actorId = "actor1";
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", new ArrayList<>());
        List<ActorEntity> list = Arrays.asList(actor);
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(list);

        actorService.getApplications(actorId, 7);
        String actorType = actorService.getActorType(actorId);
        assertEquals("TYPE1", actorType);
    }

    // 20. Test getActorMetrics – an end-to-end aggregation of several methods.
    @Test
    public void testGetActorMetrics() {
        String actorId = "actor1";
        // Create two tasks: one completed and one assigned.
        TaskEntity task1 = createTask("task1", 2, 20, "COMPLETED");
        TaskEntity task2 = createTask("task2", 1, 10, "NEW");
        ActorEntity actor = createActor(actorId, "TYPE1", "app1", Arrays.asList(task1, task2));
        List<ActorEntity> actorList = Arrays.asList(actor);

        // Stub for actorDocuments
        when(actorDao.findAllByActorIdAndLastUpdatedAtAfter(eq(actorId), any(Date.class))).thenReturn(actorList);
        // Stub for allDocuments using actorType "TYPE1"
        when(actorDao.findAllApplications(eq("TYPE1"), any(Date.class))).thenReturn(actorList);

        Map<String, Object> metrics = actorService.getActorMetrics(actorId, 7);
        assertNotNull(metrics);
        // Check that expected keys exist.
        assertTrue(metrics.containsKey("average_task_time_across_applications"));
        assertTrue(metrics.containsKey("total_tasks_completed"));
        assertTrue(metrics.containsKey("tasks_assigned"));
        assertTrue(metrics.containsKey("threshold_average_task_time"));
        assertTrue(metrics.containsKey("task_efficiency_score"));
        assertTrue(metrics.containsKey("fastest_and_slowest_task"));
        assertTrue(metrics.containsKey("most_and_least_retried_task"));
        assertTrue(metrics.containsKey("average_retries"));
        assertTrue(metrics.containsKey("average_retries_threshold"));
        assertEquals("TYPE1", metrics.get("actor_type"));
    }
}