package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sla")
public class SlaController {

    private final SlaServiceImpl slaService;

    public SlaController(SlaServiceImpl slaService) {
        this.slaService = slaService;
    }

    @GetMapping("/funnel/average-time")
    public ResponseEntity<Map<String, Double>> getAverageTimePerFunnel() {
        return ResponseEntity.ok(slaService.calculateAverageTimePerFunnel());
    }

    @GetMapping("/task/average-time")
    public ResponseEntity<Map<String, Double>> getAverageTimePerTask() {
        return ResponseEntity.ok(slaService.calculateAverageTimePerTask());
    }

    @GetMapping("/task/sendback-frequency")
    public ResponseEntity<Map<String, Long>> getSendbackFrequency() {
        return ResponseEntity.ok(slaService.calculateSendbackFrequency());
    }
}
