package com.cars24.taskmanagement.backend.ActivityLog;


import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.dao.impl.SendbackConfigDao;
import com.cars24.taskmanagement.backend.data.entity.LoanDurationEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import com.cars24.taskmanagement.backend.data.response.applicationDto.*;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;




import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.dao.impl.SendbackConfigDao;
import com.cars24.taskmanagement.backend.data.entity.LoanDurationEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import com.cars24.taskmanagement.backend.data.response.applicationDto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationDao taskExecutionDao;

    @Mock
    private SendbackConfigDao sendbackConfigDao;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private static final String APPLICATION_ID = "test-app-id";
    private static final String FUNNEL_NAME = "TEST_FUNNEL";
    private Date testDate;

    @BeforeEach
    void setUp() {
        testDate = new Date();
    }

    @Test
    void getTasksByApplicationId_Success() {
        // Arrange
        TaskExecutionLogEntity mockTask = createMockRegularTask();
        List<TaskExecutionLogEntity> mockTasks = Collections.singletonList(mockTask);

        when(taskExecutionDao.findTasksByApplicationIdSortedByUpdatedAt(APPLICATION_ID))
                .thenReturn(mockTasks);

        // Act
        ListFunnelGroupResponse response = applicationService.getTasksByApplicationId(APPLICATION_ID);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getFunnelGroupResponses());
        assertFalse(response.getFunnelGroupResponses().isEmpty());

        FunnelGroupResponse firstGroup = response.getFunnelGroupResponses().get(0);
        assertEquals(FUNNEL_NAME, firstGroup.getFunnelName());
        assertNotNull(firstGroup.getTasks());
        assertFalse(firstGroup.getTasks().isEmpty());

        TaskDetailsResponse firstTask = firstGroup.getTasks().get(0);
        assertEquals(mockTask.getTaskId(), firstTask.getTaskId());
        assertEquals(mockTask.getFunnel(), firstTask.getFunnel());
        assertEquals(mockTask.getStatus(), firstTask.getStatus());

        verify(taskExecutionDao).findTasksByApplicationIdSortedByUpdatedAt(APPLICATION_ID);
    }

    @Test
    void getTasksByApplicationId_EmptyTasks() {
        // Arrange
        when(taskExecutionDao.findTasksByApplicationIdSortedByUpdatedAt(APPLICATION_ID))
                .thenReturn(Collections.emptyList());

        // Act
        ListFunnelGroupResponse response = applicationService.getTasksByApplicationId(APPLICATION_ID);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getFunnelGroupResponses());
        assertTrue(response.getFunnelGroupResponses().isEmpty());
        verify(taskExecutionDao).findTasksByApplicationIdSortedByUpdatedAt(APPLICATION_ID);
    }

    @Test
    void getTasksGroupedByFunnel_Success() {
        // Arrange
        Map<String, Object> mockData = new HashMap<>();
        List<TaskExecutionLogEntity> tasks = Arrays.asList(
                createMockRegularTask(),
                createMockSendbackTask()
        );
        mockData.put("tasks", tasks);
        mockData.put("loanDurationEntity", createMockLoanDuration());

        when(taskExecutionDao.findTasksAndLoanDurationByApplicationId(APPLICATION_ID))
                .thenReturn(mockData);

        // Act
        Map<String, Object> response = applicationService.getTasksGroupedByFunnel(APPLICATION_ID);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("tasksGroupedByFunnel"));
        assertTrue(response.containsKey("sendbackTasks"));
        assertTrue(response.containsKey("latestTaskState"));

        @SuppressWarnings("unchecked")
        Map<String, Object> tasksGroupedByFunnel = (Map<String, Object>) response.get("tasksGroupedByFunnel");
        assertTrue(tasksGroupedByFunnel.containsKey(FUNNEL_NAME));
    }

    private TaskExecutionLogEntity createMockRegularTask() {
        TaskExecutionLogEntity task = new TaskExecutionLogEntity();
        task.setId("task-1");
        task.setTaskId("regular-task-id");
        task.setFunnel(FUNNEL_NAME);
        task.setOrder(1);
        task.setApplicationId(APPLICATION_ID);
        task.setActorId("actor-1");
        task.setStatus("COMPLETED");
        task.setHandledBy("test-user");
        task.setCreatedAt(testDate);
        task.setUpdatedAt(testDate);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test-key", "test-value");
        task.setMetadata(metadata);

        return task;
    }

    private TaskExecutionLogEntity createMockSendbackTask() {
        TaskExecutionLogEntity task = new TaskExecutionLogEntity();
        task.setId("sendback-1");
        task.setTaskId("sendback");
        task.setFunnel(FUNNEL_NAME);
        task.setOrder(2);
        task.setApplicationId(APPLICATION_ID);
        task.setActorId("actor-2");
        task.setStatus("SENDBACK");
        task.setHandledBy("test-user");
        task.setCreatedAt(testDate);
        task.setUpdatedAt(testDate);
        task.setRequestId("request-1");

        Map<String, Object> sendbackMetadata = new HashMap<>();
        sendbackMetadata.put("key", "sendback-key");
        sendbackMetadata.put("sourceLoanStage", "TEST_STAGE");
        sendbackMetadata.put("sourceSubModule", "TEST_MODULE");
        task.setSendbackMetadata(sendbackMetadata);

        return task;
    }

    private LoanDurationEntity createMockLoanDuration() {
        LoanDurationEntity entity = new LoanDurationEntity();
        entity.setId("duration-1");
        entity.setApplicationId(APPLICATION_ID);
        entity.setChannel("TEST_CHANNEL");

        LoanDurationEntity.Task task = new LoanDurationEntity.Task();
        task.setTaskId("regular-task-id");
        task.setDuration(1000L);
        task.setSendbacks(0);
        task.setVisited(1);
        task.setNew_time("2023-01-01");
        task.setUpdatedAt("2023-01-01");

        entity.setSourcing(Collections.singletonList(task));
        entity.setCredit(new ArrayList<>());
        entity.setConversion(new ArrayList<>());
        entity.setFulfillment(new ArrayList<>());

        return entity;
    }

    @Test
    void testSendbackTaskProcessing() {
        // Arrange
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("tasks", Collections.singletonList(createMockSendbackTask()));
        mockData.put("loanDurationEntity", createMockLoanDuration());

        when(taskExecutionDao.findTasksAndLoanDurationByApplicationId(APPLICATION_ID))
                .thenReturn(mockData);

        // Act
        Map<String, Object> response = applicationService.getTasksGroupedByFunnel(APPLICATION_ID);

        // Assert
        assertNotNull(response);
        @SuppressWarnings("unchecked")
        Map<String, List<TaskResponse>> sendbackTasks = (Map<String, List<TaskResponse>>) response.get("sendbackTasks");
        assertFalse(sendbackTasks.isEmpty());
        assertTrue(sendbackTasks.containsKey("request-1"));
    }

    @Test
    void testLatestTaskState() {
        // Arrange
        Map<String, Object> mockData = new HashMap<>();
        TaskExecutionLogEntity task = createMockRegularTask();
        mockData.put("tasks", Collections.singletonList(task));
        mockData.put("loanDurationEntity", createMockLoanDuration());

        when(taskExecutionDao.findTasksAndLoanDurationByApplicationId(APPLICATION_ID))
                .thenReturn(mockData);

        // Act
        Map<String, Object> response = applicationService.getTasksGroupedByFunnel(APPLICATION_ID);

        // Assert
        assertNotNull(response);
        @SuppressWarnings("unchecked")
        Map<String, Object> latestTaskState = (Map<String, Object>) response.get("latestTaskState");
        assertNotNull(latestTaskState);
        assertEquals("regular-task-id", latestTaskState.get("taskId"));
        assertEquals("COMPLETED", latestTaskState.get("status"));
    }
}