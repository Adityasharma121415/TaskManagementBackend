package com.cars24.taskmanagement.backend.controller;



import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sla")
public class SlaController {

    private final SlaServiceImpl slaService;

    public SlaController(SlaServiceImpl slaService) {
        this.slaService = slaService;
    }

    @GetMapping("/{channel}")
    public ResponseEntity<SlaResponse> getSlaMetrics(@PathVariable String channel) {
        return ResponseEntity.ok(slaService.getSlaMetricsByChannel(channel));
    }
}
