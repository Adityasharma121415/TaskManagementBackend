package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import com.cars24.taskmanagement.backend.service.impl.TaskExecutionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

        import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/task-execution")
@RequiredArgsConstructor
public class TaskExecutionController {

    private final TaskExecutionServiceImpl taskExecutionService;

    @GetMapping
    public List<TaskExecutionEntity> getAllTasks() {
        return taskExecutionService.getAllTasks();
    }

    @GetMapping("/{id}")
    public Optional<TaskExecutionEntity> getTask(@PathVariable String id) {
        return taskExecutionService.getTaskById(id);
    }

    @PostMapping
    public TaskExecutionEntity createTask(@RequestBody TaskExecutionEntity task) {
        // Set createdAt and updatedAt
        task.setCreatedAt(new java.util.Date());
        task.setUpdatedAt(new java.util.Date());
        return taskExecutionService.saveTask(task);
    }

    @PutMapping("/{id}")
    public TaskExecutionEntity updateTask(@PathVariable String id, @RequestBody TaskExecutionEntity task) {
        task.setId(id);
        task.setUpdatedAt(new java.util.Date());
        return taskExecutionService.saveTask(task);
    }
}
