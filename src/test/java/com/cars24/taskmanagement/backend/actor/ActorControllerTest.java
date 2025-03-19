package com.cars24.taskmanagement.backend.actor;
import com.cars24.taskmanagement.backend.controller.ActorController;
import com.cars24.taskmanagement.backend.service.impl.ActorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActorControllerTest {

    @Mock
    private ActorServiceImpl actorService;

    @InjectMocks
    private ActorController actorController;

    private String actorId;
    private int days;
    private Map<String, Object> mockMetrics;

    @BeforeEach
    void setUp() {
        actorId = "actor123";
        days = 7;
        mockMetrics = new HashMap<>();
        mockMetrics.put("tasksCompleted", 10);
        mockMetrics.put("averageTimePerTask", 5.5);
    }

    @Test
    void testGetActorPerformance_Success() {
        // Arrange
        when(actorService.getActorMetrics(actorId, days)).thenReturn(mockMetrics);

        // Act
        ResponseEntity<Map<String, Object>> response = actorController.getActorPerformance(actorId, days);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockMetrics, response.getBody());
        verify(actorService, times(1)).getActorMetrics(actorId, days);
    }

    @Test
    void testGetActorPerformance_EmptyMetrics() {
        // Arrange
        when(actorService.getActorMetrics(actorId, days)).thenReturn(new HashMap<>());

        // Act
        ResponseEntity<Map<String, Object>> response = actorController.getActorPerformance(actorId, days);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(0, response.getBody().size());
        verify(actorService, times(1)).getActorMetrics(actorId, days);
    }

    @Test
    void testGetActorPerformance_NullMetrics() {
        // Arrange
        when(actorService.getActorMetrics(actorId, days)).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = actorController.getActorPerformance(actorId, days);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(null, response.getBody());
        verify(actorService, times(1)).getActorMetrics(actorId, days);
    }
}