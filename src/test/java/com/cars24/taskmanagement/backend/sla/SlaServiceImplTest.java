package com.cars24.taskmanagement.backend.sla;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.exceptions.SlaException;
import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlaServiceImplTest {

    @Mock
    private SlaDaoImpl slaDao;

    @InjectMocks
    private SlaServiceImpl slaService;


    private TaskExecutionTimeEntity createTaskExecutionEntity() {
        TaskExecutionTimeEntity entity = new TaskExecutionTimeEntity();
        Instant baseTime = Instant.now();


        SubTaskEntity sourcingTask = new SubTaskEntity("sourcing_task1", baseTime);
        sourcingTask.updateStatus("COMPLETED", baseTime.plusMillis(1000));

        SubTaskEntity creditTask = new SubTaskEntity("credit_task1", baseTime);
        creditTask.updateStatus("COMPLETED", baseTime.plusMillis(2000));

        SubTaskEntity conversionTask = new SubTaskEntity("conversion_task1", baseTime);
        conversionTask.updateStatus("COMPLETED", baseTime.plusMillis(3000));

        SubTaskEntity fulfillmentTask = new SubTaskEntity("fulfillment_task1", baseTime);
        fulfillmentTask.updateStatus("COMPLETED", baseTime.plusMillis(4000));


        entity.setSourcing(Collections.singletonList(sourcingTask));
        entity.setCredit(Collections.singletonList(creditTask));
        entity.setConversion(Collections.singletonList(conversionTask));
        entity.setFulfillment(Collections.singletonList(fulfillmentTask));

        return entity;
    }

    @Test
    public void testGetSlaMetricsByChannel_NoDataFound() {
        when(slaDao.getTasksByChannel("nonexistent")).thenReturn(Collections.emptyList());
        assertThrows(SlaException.class, () -> slaService.getSlaMetricsByChannel("nonexistent"));
    }

    @Test
    public void testGetSlaMetricsByChannel_ValidData() {
        TaskExecutionTimeEntity entity = createTaskExecutionEntity();
        when(slaDao.getTasksByChannel("testChannel")).thenReturn(List.of(entity));

        SlaResponse response = slaService.getSlaMetricsByChannel("testChannel");
        assertNotNull(response);
        assertNotNull(response.getFunnels());


        assertTrue(response.getFunnels().containsKey("sourcing"));
        assertTrue(response.getFunnels().containsKey("credit"));
        assertTrue(response.getFunnels().containsKey("conversion"));
        assertTrue(response.getFunnels().containsKey("fulfillment"));


        SlaResponse.Funnel sourcingFunnel = response.getFunnels().get("sourcing");
        assertNotNull(sourcingFunnel);
        assertTrue(sourcingFunnel.getTasks().containsKey("sourcing_task1"));

        SlaResponse.Task task = sourcingFunnel.getTasks().get("sourcing_task1");
        assertNotNull(task);

        assertEquals(SlaResponse.formatDuration(1000L), task.getTimeTaken());
        assertEquals(0L, task.getNoOfSendbacks());


        long expectedTAT = 1000L + 2000L + 3000L + 4000L;
        assertEquals(SlaResponse.formatDuration(expectedTAT), response.getAverageTAT());
    }
}
