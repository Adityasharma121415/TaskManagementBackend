package com.cars24.taskmanagement.backend.sla;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.exceptions.SlaException;
import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SlaServiceImplTest {

    @Mock
    private SlaDaoImpl slaDao;

    @InjectMocks
    private SlaServiceImpl slaService;

    private TaskExecutionTimeEntity executionEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Create a sample TaskExecutionTimeEntity for testing
        executionEntity = new TaskExecutionTimeEntity();
        executionEntity.setApplicationId("app1");
        executionEntity.setEntityId("entity1");
        executionEntity.setChannel("D2C");
        // Set recordDate to 1 day ago
        executionEntity.setRecordDate(Instant.now().minus(1, ChronoUnit.DAYS));

        // Create a sample SubTaskEntity in the "sourcing" funnel
        SubTaskEntity subTask = new SubTaskEntity("task1", Instant.now().minus(60, ChronoUnit.MINUTES));
        subTask.setDuration(3600000L); // 1 hour in ms
        subTask.setSendbacks(1);
        subTask.setVisited(1);
        subTask.setStatusoftask("COMPLETED");
        List<SubTaskEntity> sourcingTasks = new ArrayList<>();
        sourcingTasks.add(subTask);
        executionEntity.setSourcing(sourcingTasks);

        // Initialize empty lists for other funnels.
        executionEntity.setCredit(new ArrayList<>());
        executionEntity.setRisk(new ArrayList<>());
        executionEntity.setConversion(new ArrayList<>());
        executionEntity.setRto(new ArrayList<>());
        executionEntity.setFulfillment(new ArrayList<>());
        executionEntity.setDisbursal(new ArrayList<>());
    }

    @Test
    void testGetSlaMetricsByChannel_NoFilter() {
        // Setup DAO to return one execution record.
        List<TaskExecutionTimeEntity> executions = Collections.singletonList(executionEntity);
        when(slaDao.getTasksByChannel("D2C")).thenReturn(executions);

        // Invoke SLA service with no filtering (days = null, status = empty)
        SlaResponse response = slaService.getSlaMetricsByChannel("D2C", null, "");

        assertNotNull(response);
        assertNotNull(response.getFunnels());
        // Assuming averageTAT should be formatted as "1 hrs" (depending on SlaResponse.formatDuration implementation)
        assertEquals("1 hrs", response.getAverageTAT().trim());
    }

    @Test
    void testGetSlaMetricsByChannel_WithDaysFilter() {
        List<TaskExecutionTimeEntity> executions = Collections.singletonList(executionEntity);
        when(slaDao.getTasksByChannel("D2C")).thenReturn(executions);

        // Use days = 7; since our recordDate is 1 day ago, it should be included.
        SlaResponse response = slaService.getSlaMetricsByChannel("D2C", 7, "");
        assertNotNull(response);
    }

    @Test
    void testGetSlaMetricsByChannel_WithStatusFilter_Approved() {
        List<TaskExecutionTimeEntity> executions = Collections.singletonList(executionEntity);
        when(slaDao.getTasksByChannel("D2C")).thenReturn(executions);

        // Our sample execution has one COMPLETED subtask so overall status should be Approved.
        SlaResponse response = slaService.getSlaMetricsByChannel("D2C", null, "Approved");
        assertNotNull(response);
    }

    @Test
    void testGetSlaMetricsByChannel_NoDataFound() {
        when(slaDao.getTasksByChannel("D2C")).thenReturn(new ArrayList<>());

        SlaException ex = assertThrows(SlaException.class, () -> {
            slaService.getSlaMetricsByChannel("D2C", 7, "Pending");
        });
        assertTrue(ex.getMessage().contains("No data found for channel"));
    }
}
