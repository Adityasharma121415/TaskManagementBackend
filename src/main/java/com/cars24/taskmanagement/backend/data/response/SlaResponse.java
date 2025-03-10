package com.cars24.taskmanagement.backend.data.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class SlaResponse {
    private Map<String, Double> averageTimePerFunnel;
    private Map<String, Double> averageTimePerTask;
    private Map<String, Long> totalSendbacksPerTask;
    private double averageTAT;
}