package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import com.cars24.taskmanagement.backend.service.TaskExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/task-execution")
@RequiredArgsConstructor
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;

    @GetMapping
    public ResponseEntity<List<TaskExecutionEntity>> getAllTasks() {
        List<TaskExecutionEntity> tasks = taskExecutionService.findAll();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskExecutionEntity> getTaskById(@PathVariable String id) {
        Optional<TaskExecutionEntity> task = taskExecutionService.findById(id);
        return task.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TaskExecutionEntity> createTask(@RequestBody TaskExecutionEntity task) {
        task.setCreatedAt(new java.util.Date());
        task.setUpdatedAt(new java.util.Date());
        TaskExecutionEntity savedTask = taskExecutionService.save(task);
        return ResponseEntity.ok(savedTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskExecutionEntity> updateTask(
            @PathVariable String id,
            @RequestBody TaskExecutionEntity task
    ) {
        task.setId(id);
        task.setUpdatedAt(new java.util.Date());
        TaskExecutionEntity updatedTask = taskExecutionService.save(task);
        return ResponseEntity.ok(updatedTask);
    }
}