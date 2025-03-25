package com.cars24.taskmanagement.backend.service.redisCache;

import com.cars24.taskmanagement.backend.constants.FileConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisCacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void storeTaskStartTime(String applicationId, String taskId, String actorId, Instant updatedAt) {
        log.info("RedisCacheService [storeTaskStartTime] {} {} {} {}", applicationId, taskId, actorId, updatedAt);
        String key = applicationId + ":" + taskId + ":" + actorId;
        redisTemplate.opsForValue().set(key, updatedAt.toString(), FileConstants.TTL, TimeUnit.HOURS);
    }

    public Instant getTaskStartTime(String applicationId, String taskId, String actorId) {
        log.info("RedisCacheService [getTaskStartTime] {} {} {}", applicationId, taskId, actorId);
        String key = applicationId + ":" + taskId + ":" + actorId;
        String storedTime = redisTemplate.opsForValue().get(key);
        return (storedTime != null) ? Instant.parse(storedTime) : null;
    }

    public void removeTaskStartTime(String applicationId, String taskId, String actorId) {
        log.info("RedisCacheService [removeTaskStartTime] {} {} {}", applicationId, taskId, actorId);
        String key = applicationId + ":" + taskId + ":" + actorId;
        redisTemplate.delete(key);
    }

    public void storeSystemTaskStartTime(String funnel, String applicationId, String taskId, Instant updatedAt){
        log.info("RedisCacheService [storeSystemTaskStartTime] {} {} {} {}", funnel, applicationId, taskId, updatedAt);
        String key = funnel + ":" + applicationId + ":" + taskId;
        redisTemplate.opsForValue().set(key, updatedAt.toString(), FileConstants.TTL, TimeUnit.HOURS);
    }

    public Instant getSystemTaskStartTime(String funnel, String applicationId, String taskId){
        log.info("RedisCacheService [getSystemTaskStartTime] {} {} {}", funnel, applicationId, taskId);
        String key = funnel + ":" + applicationId + ":" + taskId;
        String storedTime = redisTemplate.opsForValue().get(key);
        return (storedTime != null) ? Instant.parse(storedTime) : null;
    }

    public void removeSystemTaskStartTime(String funnel, String applicationId, String taskId) {
        log.info("RedisCacheService [removeSystemTaskStartTime] {} {} {}", funnel, applicationId, taskId);
        String key = funnel + ":" + applicationId + ":" + taskId;
        redisTemplate.delete(key);
    }
}
