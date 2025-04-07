package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.service.impl.SlaServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/SLAMonitoring")
@RequiredArgsConstructor
@Slf4j
public class SlaController {

    private final SlaServiceImpl slaService;


    @GetMapping("/time/{channel}/{days}/{appStatusFilter}/{taskStatusFilter}")
    public SlaResponse getSlaByChannelAndStatus(
            @PathVariable String channel,
            @PathVariable int days,
            @PathVariable String appStatusFilter,
            @PathVariable String taskStatusFilter) {
        log.info("Received request for SLA metrics of channel: {} for last {} days with overall status: {} and task status filter: {}",
                channel, days, appStatusFilter, taskStatusFilter);
        return slaService.getSlaMetricsByChannel(channel, days, appStatusFilter, taskStatusFilter);
    }
}
