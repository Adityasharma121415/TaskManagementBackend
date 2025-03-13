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
        // Create a dummy SlaResponse.
        Map<String, SlaResponse.Funnel> funnels = new LinkedHashMap<>();

        // Populate the "sourcing" funnel with a dummy task.
        Map<String, SlaResponse.Task> sourcingTasks = new LinkedHashMap<>();
        sourcingTasks.put("sourcing_task1", new SlaResponse.Task(SlaResponse.formatDuration(1000L), 1L));
        funnels.put("sourcing", new SlaResponse.Funnel(SlaResponse.formatDuration(1000L), sourcingTasks));

        // Add the remaining funnels (even if empty or with dummy durations).
        funnels.put("credit", new SlaResponse.Funnel(SlaResponse.formatDuration(2000L), new LinkedHashMap<>()));
        funnels.put("conversion", new SlaResponse.Funnel(SlaResponse.formatDuration(3000L), new LinkedHashMap<>()));
        funnels.put("fulfillment", new SlaResponse.Funnel(SlaResponse.formatDuration(4000L), new LinkedHashMap<>()));

        // Here, the response key is "averageTAT" (not "totalTat")
        String averageTAT = SlaResponse.formatDuration(1000L + 2000L + 3000L + 4000L);
        SlaResponse dummyResponse = new SlaResponse(funnels, averageTAT);

        when(slaService.getSlaMetricsByChannel("testChannel")).thenReturn(dummyResponse);

        mockMvc.perform(get("/SLAMonitoring/time/testChannel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Use the actual JSON key "averageTAT" instead of "totalTat".
                .andExpect(jsonPath("$.averageTAT").value(averageTAT))
                // Updated JSON property name from "taskTime" to "timeTaken".
                .andExpect(jsonPath("$.funnels.sourcing.tasks.sourcing_task1.timeTaken")
                        .value(SlaResponse.formatDuration(1000L)))
                .andExpect(jsonPath("$.funnels.sourcing.tasks.sourcing_task1.noOfSendbacks").value(1));
    }
}
