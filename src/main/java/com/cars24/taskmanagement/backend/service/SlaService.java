package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.response.SlaResponse;

public interface SlaService {
    public SlaResponse getSlaMetricsByChannel(String channel);

    SlaResponse getSlaMetricsByChannel(String channel, Integer days);

    SlaResponse getSlaMetricsByChannel(String channel, Integer days, String status);
}
