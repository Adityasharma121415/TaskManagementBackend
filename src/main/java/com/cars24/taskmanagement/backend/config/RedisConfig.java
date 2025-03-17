package com.cars24.taskmanagement.backend.config;

import com.cars24.taskmanagement.backend.constants.FileConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(FileConstants.REDIS_HOST);
        config.setPort(FileConstants.REDIS_PORT);
        config.setPassword(FileConstants.REDIS_PASSWORD);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(FileConstants.TOTAL_CONNECTIONS);
        poolConfig.setMaxIdle(FileConstants.MAX_IDLE);
        poolConfig.setMinIdle(FileConstants.MIN_IDLE);

        JedisClientConfiguration jedisClientConfig = JedisClientConfiguration.builder()
                .build();

        return new JedisConnectionFactory(config, jedisClientConfig);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate(jedisConnectionFactory());
    }
}