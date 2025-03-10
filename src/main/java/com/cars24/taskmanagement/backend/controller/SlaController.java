package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.response.SlaTimeResponse;
import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/SLAMonitoring")
@RequiredArgsConstructor
public class SlaController {

    private final SlaServiceImpl slaService;

    @GetMapping("/time/{channel}")
    public SlaTimeResponse getSlaByChannel(@PathVariable String channel) {
        return slaService.getSlaMetricsByChannel(channel);
    }
}
