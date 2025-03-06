package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import com.cars24.taskmanagement.backend.service.TaskExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskExecutionTimeServiceImpl extends AbstractMongoEventListener<TaskExecutionEntity> {

    private final TaskExecutionService taskExecutionService;

    @Override
    public void onAfterSave(AfterSaveEvent<TaskExecutionEntity> event) {
        TaskExecutionEntity task = event.getSource();
        taskExecutionService.processTaskExecution(task);
    }
}
