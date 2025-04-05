package com.cars24.taskmanagement.backend.ActivityLog;

import com.cars24.taskmanagement.backend.controller.ApplicationController;
import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.*;
import com.cars24.taskmanagement.backend.service.impl.ApplicationGanntService;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ApplicationControllerTest {

    @Mock
    private ApplicationServiceImpl applicationService;

    @Mock
    private ApplicationGanntService applicationGanntService;
    @InjectMocks
    private ApplicationController applicationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTasksByApplicationId() {
        // Arrange
        String applicationId = "app123";
        Map<String, Object> mockTasks = new HashMap<>();
        mockTasks.put("task1", "Task 1 details");
        mockTasks.put("task2", "Task 2 details");

        when(applicationService.getTasksGroupedByFunnel(applicationId)).thenReturn(mockTasks);

        // Act
        ResponseEntity<ApiResponse> responseEntity = applicationController.getTasksByApplicationId(applicationId);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatusCode());
        assertEquals(true, responseEntity.getBody().isSuccess());
        assertEquals("Tasks retrieved successfully", responseEntity.getBody().getMessage());
        assertEquals("APPUSER200", responseEntity.getBody().getService());
        assertEquals(mockTasks, responseEntity.getBody().getData());

        verify(applicationService, times(1)).getTasksGroupedByFunnel(applicationId);
    }

    @Test
    void testGetTasksByApplicationIdDC() {
        // Arrange
        String applicationId = "app123";

        // Create mock StatusLogResponse objects
        StatusLogResponse statusLog1 = new StatusLogResponse("Pending", new Date());
        StatusLogResponse statusLog2 = new StatusLogResponse("Completed", new Date());

        // Create mock TaskDetailsResponse objects
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");

        TaskDetailsResponse task1 = new TaskDetailsResponse(
                "Funnel 1", "actor1", "Pending", new Date(), "task1", "targetTask1", 2, 100, metadata
        );
        TaskDetailsResponse task2 = new TaskDetailsResponse(
                "Funnel 2", "actor2", "Completed", new Date(), "task2", "targetTask2", 1, 200, metadata
        );

        // Create mock TaskResponse objects
        TaskResponse taskResponse1 = new TaskResponse(
                "task1", 1, "actor1", new Date(), Arrays.asList(statusLog1, statusLog2), "targetTask1", 100, 2, 1, "stage1", "module1"
        );
        TaskResponse taskResponse2 = new TaskResponse(
                "task2", 2, "actor2", new Date(), Arrays.asList(statusLog2), "targetTask2", 200, 1, 0, "stage2", "module2"
        );

        // Create mock FunnelGroupResponse objects
        FunnelGroupResponse funnelGroup1 = new FunnelGroupResponse(
                "Funnel 1", Arrays.asList(task1), 1000L
        );
        FunnelGroupResponse funnelGroup2 = new FunnelGroupResponse(
                "Funnel 2", Arrays.asList(task2), 2000L
        );

        // Create a mock ListFunnelGroupResponse
        List<FunnelGroupResponse> funnelGroupResponses = Arrays.asList(funnelGroup1, funnelGroup2);
        ListFunnelGroupResponse mockResponse = new ListFunnelGroupResponse(funnelGroupResponses);

        when(applicationGanntService.getTasksByApplicationId(applicationId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<ApiResponse> responseEntity = applicationController.getTasksByApplicationIdDC(applicationId);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatusCode());
        assertEquals(true, responseEntity.getBody().isSuccess());
        assertEquals("Tasks retrieved successfully", responseEntity.getBody().getMessage());
        assertEquals("APPUSER200", responseEntity.getBody().getService());
        assertEquals(mockResponse, responseEntity.getBody().getData());

        verify(applicationGanntService, times(1)).getTasksByApplicationId(applicationId);
    }
}