package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/SLAMonitoring")
@RequiredArgsConstructor
@CrossOrigin(origins="http://localhost:5173")
public class SlaController {

    private final SlaServiceImpl slaService;

    @GetMapping("/time/{channel}")
    public SlaResponse getSlaByChannel(@PathVariable String channel) {
        return slaService.getSlaMetricsByChannel(channel);
    }
}
