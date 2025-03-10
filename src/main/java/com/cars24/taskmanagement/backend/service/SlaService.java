package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.response.SlaTimeResponse;

public interface SlaService {
    public SlaTimeResponse getSlaMetricsByChannel(String channel);
}
