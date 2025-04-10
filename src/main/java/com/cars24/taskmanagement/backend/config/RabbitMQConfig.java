package com.cars24.taskmanagement.backend.config;

import com.cars24.taskmanagement.backend.constants.FileConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        // Uncomment if authentication is needed
        // connectionFactory.setUsername("guest");
        // connectionFactory.setPassword("guest");
        return connectionFactory;
    }

    @Bean
    public TopicExchange taskExecutionExchange() {
        return new TopicExchange(FileConstants.EXCHANGE);
    }

    @Bean
    public Queue taskPriorityQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", 10);
        return new Queue(FileConstants.TASK_EXEC_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue taskExecutionLogQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", 10);
        return new Queue(FileConstants.TASK_EXEC_LOG_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding taskExecutionbinding(Queue taskPriorityQueue, TopicExchange taskExecutionExchange) {
        return BindingBuilder.bind(taskPriorityQueue)
                .to(taskExecutionExchange)
                .with(FileConstants.TASK_EXEC_ROUTING_KEY);
    }

    @Bean
    public Binding taskExecutionLogbinding(Queue taskExecutionLogQueue, TopicExchange exchange) {
        return BindingBuilder.bind(taskExecutionLogQueue)
                .to(exchange)
                .with(FileConstants.TASK_EXEC_LOG_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(classMapper());
        return converter;
    }

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("com.cars24.taskmanagement.backend.service.changeStreams");
        return classMapper;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
