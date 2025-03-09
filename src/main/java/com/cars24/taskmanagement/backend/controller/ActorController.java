package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.service.impl.ActorServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/actorMetrics")
@RequiredArgsConstructor
@Slf4j
public class ActorController {

    private final ActorServiceImpl actorService;

    @GetMapping(path = "{actorId}")
    public ResponseEntity getActorPerformance(@PathVariable String actorId){
        log.info("ActorController [getActorPerformance] {}", actorId);
        return ResponseEntity.ok().body(actorService.getAverageDuration(actorId));
    }
}
