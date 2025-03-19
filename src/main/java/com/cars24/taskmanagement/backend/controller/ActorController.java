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

    @GetMapping(path = "{actorId}/{days}")
    public ResponseEntity getActorPerformance(@PathVariable String actorId, @PathVariable int days){
        log.info("ActorController [getActorPerformance] {} {}", actorId, days);

        if(actorId == null || !actorId.matches("\\d+")){
            return ResponseEntity.badRequest().body(Map.of("Error", "Invalid actorId: must be numeric and greater than 0"));
        }

        if(days < 7 || days > 90){
            return ResponseEntity.badRequest().body(Map.of("Error", "Invalid number of days: must be between 7 and 90"));
        }

        return ResponseEntity.ok().body(actorService.getActorMetrics(actorId, days));
    }
}
