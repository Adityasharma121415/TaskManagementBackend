package com.cars24.taskmanagement.backend.sla;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlaDaoImplTest {

    @Mock
    private TaskExecutionTimeRepository repository;

    @InjectMocks
    private SlaDaoImpl slaDao;

    @Test
    public void testGetTasksByChannel() {
        List<TaskExecutionTimeEntity> dummyList = Collections.emptyList();
        when(repository.findByChannel("testChannel")).thenReturn(dummyList);

        List<TaskExecutionTimeEntity> result = slaDao.getTasksByChannel("testChannel");
        assertEquals(dummyList, result);
    }
}
