package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins="http://localhost:5173")
@RestController
@RequestMapping("/SLAMonitoring")
@RequiredArgsConstructor
@Slf4j
public class SlaController {

    private final SlaServiceImpl slaService;

    @GetMapping("/time/{channel}")
    public SlaResponse getSlaByChannel(@PathVariable String channel) {
        log.info("Received request for SLA metrics of channel: {}", channel);
        return slaService.getSlaMetricsByChannel(channel);
    }
}
