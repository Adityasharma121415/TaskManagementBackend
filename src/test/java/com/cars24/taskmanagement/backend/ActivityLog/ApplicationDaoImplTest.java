package com.cars24.taskmanagement.backend.ActivityLog;




import com.cars24.taskmanagement.backend.data.dao.impl.ApplicationDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.LoanDurationEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import com.cars24.taskmanagement.backend.data.repository.LoanDurationRepository;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationDaoImplTest {

    @Mock
    private TaskExecutionLogRepository taskExecutionLogRepository;

    @Mock
    private LoanDurationRepository loanDurationRepository;

    @InjectMocks
    private ApplicationDaoImpl applicationDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindByApplicationId() {
        // Arrange
        String applicationId = "app123";
        List<TaskExecutionLogEntity> mockTasks = Arrays.asList(
                new TaskExecutionLogEntity(),
                new TaskExecutionLogEntity()
        );

        when(taskExecutionLogRepository.findByApplicationId(applicationId)).thenReturn(mockTasks);

        // Act
        List<TaskExecutionLogEntity> result = applicationDao.findByApplicationId(applicationId);

        // Assert
        assertEquals(mockTasks, result);
        verify(taskExecutionLogRepository, times(1)).findByApplicationId(applicationId);
    }

    @Test
    void testFindTasksByApplicationIdSortedByUpdatedAt() {
        // Arrange
        String applicationId = "app123";
        List<TaskExecutionLogEntity> mockTasks = Arrays.asList(
                new TaskExecutionLogEntity(),
                new TaskExecutionLogEntity()
        );

        when(taskExecutionLogRepository.findTasksByApplicationIdSortedByUpdatedAt(applicationId)).thenReturn(mockTasks);

        // Act
        List<TaskExecutionLogEntity> result = applicationDao.findTasksByApplicationIdSortedByUpdatedAt(applicationId);

        // Assert
        assertEquals(mockTasks, result);
        verify(taskExecutionLogRepository, times(1)).findTasksByApplicationIdSortedByUpdatedAt(applicationId);
    }

    @Test
    void testFindTasksAndLoanDurationByApplicationId() {
        // Arrange
        String applicationId = "app123";

        // Mock TaskExecutionLogEntity list
        List<TaskExecutionLogEntity> mockTasks = Arrays.asList(
                new TaskExecutionLogEntity(),
                new TaskExecutionLogEntity()
        );

        // Mock LoanDurationEntity
        LoanDurationEntity.Task task1 = new LoanDurationEntity.Task();
        task1.setTaskId("task1");
        task1.setNew_time("2023-10-01T00:00:00Z");
        task1.setUpdatedAt("2023-10-01T00:00:00Z");
        task1.setSendbacks(2);
        task1.setDuration(1000L);
        task1.setVisited(1);

        LoanDurationEntity.Task task2 = new LoanDurationEntity.Task();
        task2.setTaskId("task2");
        task2.setNew_time("2023-10-02T00:00:00Z");
        task2.setUpdatedAt("2023-10-02T00:00:00Z");
        task2.setSendbacks(1);
        task2.setDuration(2000L);
        task2.setVisited(0);

        LoanDurationEntity mockLoanDuration = new LoanDurationEntity();
        mockLoanDuration.setId("loan1");
        mockLoanDuration.setApplicationId(applicationId);
        mockLoanDuration.setEntityId("entity1");
        mockLoanDuration.setChannel("channel1");
        mockLoanDuration.setSourcing(Arrays.asList(task1, task2));
        mockLoanDuration.setCredit(Collections.singletonList(task1));
        mockLoanDuration.setConversion(Collections.singletonList(task2));
        mockLoanDuration.setFulfillment(Arrays.asList(task1, task2));

        when(taskExecutionLogRepository.findByApplicationId(applicationId)).thenReturn(mockTasks);
        when(loanDurationRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(mockLoanDuration));

        // Act
        Map<String, Object> result = applicationDao.findTasksAndLoanDurationByApplicationId(applicationId);

        // Assert
        assertNotNull(result);
        assertEquals(mockTasks, result.get("tasks"));
        assertEquals(mockLoanDuration, result.get("loanDurationEntity"));
        verify(taskExecutionLogRepository, times(1)).findByApplicationId(applicationId);
        verify(loanDurationRepository, times(1)).findByApplicationId(applicationId);
    }

    @Test
    void testFindTasksAndLoanDurationByApplicationId_NoLoanDuration() {
        // Arrange
        String applicationId = "app123";

        // Mock TaskExecutionLogEntity list
        List<TaskExecutionLogEntity> mockTasks = Arrays.asList(
                new TaskExecutionLogEntity(),
                new TaskExecutionLogEntity()
        );

        when(taskExecutionLogRepository.findByApplicationId(applicationId)).thenReturn(mockTasks);
        when(loanDurationRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = applicationDao.findTasksAndLoanDurationByApplicationId(applicationId);

        // Assert
        assertNotNull(result);
        assertEquals(mockTasks, result.get("tasks"));
        assertNull(result.get("loanDurationEntity"));
        verify(taskExecutionLogRepository, times(1)).findByApplicationId(applicationId);
        verify(loanDurationRepository, times(1)).findByApplicationId(applicationId);
    }
}