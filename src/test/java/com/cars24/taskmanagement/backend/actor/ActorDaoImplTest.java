package com.cars24.taskmanagement.backend.actor;


import com.cars24.taskmanagement.backend.data.dao.impl.ActorDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.ActorEntity;
import com.cars24.taskmanagement.backend.data.repository.ActorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActorDaoImplTest {

    @Mock
    private ActorRepository actorRepository;

    @InjectMocks
    private ActorDaoImpl actorDao;

    private String actorId;
    private String actorType;
    private Date filteredDate;
    private Date pastDate;
    private List<ActorEntity> mockActorEntities;

    @BeforeEach
    void setUp() {
        actorId = "actor123";
        actorType = "typeA";
        filteredDate = new Date();
        pastDate = new Date(System.currentTimeMillis() - 86400000); // 1 day ago

        // Create mock ActorEntity objects using the builder pattern
        mockActorEntities = Arrays.asList(
                ActorEntity.builder()
                        .actorId("actor123")
                        .actorType("typeA")
                        .applicationId("app1")
                        .lastUpdatedAt(Instant.now())
                        .build(),
                ActorEntity.builder()
                        .actorId("actor123")
                        .actorType("typeA")
                        .applicationId("app2")
                        .lastUpdatedAt(Instant.now())
                        .build()
        );
    }

    @Test
    void testFindAllByActorIdAndLastUpdatedAtAfter() {
        // Arrange
        when(actorRepository.findAllByActorIdAndLastUpdatedAtAfter(actorId, filteredDate))
                .thenReturn(mockActorEntities);

        // Act
        List<ActorEntity> result = actorDao.findAllByActorIdAndLastUpdatedAtAfter(actorId, filteredDate);

        // Assert
        assertEquals(mockActorEntities, result);
        verify(actorRepository, times(1))
                .findAllByActorIdAndLastUpdatedAtAfter(actorId, filteredDate);
    }

    @Test
    void testFindAllApplications() {
        // Arrange
        when(actorRepository.findAllByLastUpdatedAtAfter(actorType, pastDate))
                .thenReturn(mockActorEntities);

        // Act
        List<ActorEntity> result = actorDao.findAllApplications(actorType, pastDate);

        // Assert
        assertEquals(mockActorEntities, result);
        verify(actorRepository, times(1))
                .findAllByLastUpdatedAtAfter(actorType, pastDate);
    }

    @Test
    void testFindAllByActorIdAndLastUpdatedAtAfter_EmptyList() {
        // Arrange
        when(actorRepository.findAllByActorIdAndLastUpdatedAtAfter(actorId, filteredDate))
                .thenReturn(List.of());

        // Act
        List<ActorEntity> result = actorDao.findAllByActorIdAndLastUpdatedAtAfter(actorId, filteredDate);

        // Assert
        assertEquals(0, result.size());
        verify(actorRepository, times(1))
                .findAllByActorIdAndLastUpdatedAtAfter(actorId, filteredDate);
    }

    @Test
    void testFindAllApplications_EmptyList() {
        // Arrange
        when(actorRepository.findAllByLastUpdatedAtAfter(actorType, pastDate))
                .thenReturn(List.of());

        // Act
        List<ActorEntity> result = actorDao.findAllApplications(actorType, pastDate);

        // Assert
        assertEquals(0, result.size());
        verify(actorRepository, times(1))
                .findAllByLastUpdatedAtAfter(actorType, pastDate);
    }
}