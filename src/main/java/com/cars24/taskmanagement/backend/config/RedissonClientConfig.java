package com.cars24.taskmanagement.backend.config;

import com.cars24.taskmanagement.backend.constants.FileConstants;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonClientConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+ FileConstants.REDIS_HOST + ":" + FileConstants.REDIS_PORT)
                .setPassword(FileConstants.REDIS_PASSWORD)
                .setConnectionMinimumIdleSize(FileConstants.REDISSON_MIN_IDLE_CONNECTIONS)
                .setConnectionPoolSize(FileConstants.TOTAL_CONNECTIONS)
                .setRetryAttempts(FileConstants.REDISSON_RETRY_ATTEMPTS);
        return Redisson.create(config);
    }
}
