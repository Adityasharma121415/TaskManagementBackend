package com.cars24.taskmanagement.backend.sla;

import com.cars24.taskmanagement.backend.controller.SlaController;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SlaController.class)
public class SlaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlaServiceImpl slaService;

    @Test
    public void testGetSlaByChannel() throws Exception {
        // Create funnels
        Map<String, SlaResponse.Funnel> funnels = new LinkedHashMap<>();

        // Sourcing tasks
        Map<String, SlaResponse.Task> sourcingTasks = new LinkedHashMap<>();
        sourcingTasks.put("sourcing_task1", new SlaResponse.Task(SlaResponse.formatDuration(1000L), 1L));
        funnels.put("sourcing", new SlaResponse.Funnel(SlaResponse.formatDuration(1000L), sourcingTasks));

        // Other funnels
        funnels.put("credit", new SlaResponse.Funnel(SlaResponse.formatDuration(2000L), new LinkedHashMap<>()));
        funnels.put("conversion", new SlaResponse.Funnel(SlaResponse.formatDuration(3000L), new LinkedHashMap<>()));
        funnels.put("fulfillment", new SlaResponse.Funnel(SlaResponse.formatDuration(4000L), new LinkedHashMap<>()));

        // Calculate average TAT
        String averageTAT = SlaResponse.formatDuration(1000L + 2000L + 3000L + 4000L);

        // Add TAT Distribution
        Map<String, SlaResponse.Distribution> tatDistribution = new LinkedHashMap<>();
        tatDistribution.put("0 - 2 hours", new SlaResponse.Distribution(2L, List.of("APP1", "APP2")));
        tatDistribution.put("2 - 5 hours", new SlaResponse.Distribution(3L, List.of("APP3", "APP4", "APP5")));

        // Add Task Distribution
        Map<String, Map<String, SlaResponse.Distribution>> taskDistribution = new LinkedHashMap<>();
        Map<String, SlaResponse.Distribution> sourcingTaskDist = new LinkedHashMap<>();
        sourcingTaskDist.put("0 - 1 hour", new SlaResponse.Distribution(1L, List.of("APP1")));
        sourcingTaskDist.put("1 - 3 hours", new SlaResponse.Distribution(2L, List.of("APP2", "APP3")));
        taskDistribution.put("sourcing_task1", sourcingTaskDist);

        // Create SlaResponse object
        SlaResponse dummyResponse = new SlaResponse(funnels, averageTAT, tatDistribution, taskDistribution);

        // Mock service call
        when(slaService.getSlaMetricsByChannel("testChannel")).thenReturn(dummyResponse);

        // Perform GET request and validate JSON response
        mockMvc.perform(get("/SLAMonitoring/time/testChannel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // Validate average TAT
                .andExpect(jsonPath("$.averageTAT").value(averageTAT))

                // Validate funnel tasks
                .andExpect(jsonPath("$.funnels.sourcing.tasks.sourcing_task1.timeTaken")
                        .value(SlaResponse.formatDuration(1000L)))
                .andExpect(jsonPath("$.funnels.sourcing.tasks.sourcing_task1.noOfSendbacks").value(1))

                // Validate TAT Distribution
                .andExpect(jsonPath("$.tatDistribution['0 - 2 hours'].count").value(2))
                .andExpect(jsonPath("$.tatDistribution['0 - 2 hours'].applicationIds[0]").value("APP1"))
                .andExpect(jsonPath("$.tatDistribution['2 - 5 hours'].count").value(3))
                .andExpect(jsonPath("$.tatDistribution['2 - 5 hours'].applicationIds[1]").value("APP4"))

                // Validate Task Distribution
                .andExpect(jsonPath("$.taskDistribution.sourcing_task1['0 - 1 hour'].count").value(1))
                .andExpect(jsonPath("$.taskDistribution.sourcing_task1['0 - 1 hour'].applicationIds[0]").value("APP1"))
                .andExpect(jsonPath("$.taskDistribution.sourcing_task1['1 - 3 hours'].count").value(2))
                .andExpect(jsonPath("$.taskDistribution.sourcing_task1['1 - 3 hours'].applicationIds[1]").value("APP3"));
    }
}
