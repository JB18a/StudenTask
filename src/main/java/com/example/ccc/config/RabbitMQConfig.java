package com.example.ccc.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TASK_QUEUE = "task.queue";
    public static final String TASK_EXCHANGE = "task.exchange";
    public static final String TASK_ROUTING_KEY = "task.routing.key";

    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";

    public static final String GRAB_TASK_QUEUE = "grab.task.queue";
    public static final String GRAB_TASK_EXCHANGE = "grab.task.exchange";
    public static final String GRAB_TASK_ROUTING_KEY = "grab.task.routing.key";

    public static final String TASK_TIMEOUT_DELAY_QUEUE = "task.timeout.delay.queue";
    public static final String TASK_TIMEOUT_DLX = "task.timeout.dlx";
    public static final String TASK_TIMEOUT_QUEUE = "task.timeout.queue";
    public static final String TASK_TIMEOUT_ROUTING_KEY = "task.timeout.routing.key";

    public static final String TASK_REMINDER_DELAY_QUEUE = "task.reminder.delay.queue";
    public static final String TASK_REMINDER_DLX = "task.reminder.dlx";
    public static final String TASK_REMINDER_QUEUE = "task.reminder.queue";
    public static final String TASK_REMINDER_ROUTING_KEY = "task.reminder.routing.key";

    public static final String GRAB_RELEASE_DELAY_QUEUE = "grab.release.delay.queue";
    public static final String GRAB_RELEASE_DLX = "grab.release.dlx";
    public static final String GRAB_RELEASE_QUEUE = "grab.release.queue";
    public static final String GRAB_RELEASE_ROUTING_KEY = "grab.release.routing.key";

    public static final String AI_ANALYSIS_QUEUE = "ai.analysis.queue";
    public static final String AI_ANALYSIS_EXCHANGE = "ai.analysis.exchange";
    public static final String AI_ANALYSIS_ROUTING_KEY = "ai.analysis.routing.key";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(TASK_QUEUE).build();
    }

    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE);
    }

    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue()).to(taskExchange()).with(TASK_ROUTING_KEY);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Queue grabTaskQueue() {
        return QueueBuilder.durable(GRAB_TASK_QUEUE).build();
    }

    @Bean
    public DirectExchange grabTaskExchange() {
        return new DirectExchange(GRAB_TASK_EXCHANGE);
    }

    @Bean
    public Binding grabTaskBinding() {
        return BindingBuilder.bind(grabTaskQueue()).to(grabTaskExchange()).with(GRAB_TASK_ROUTING_KEY);
    }

    @Bean
    public DirectExchange taskTimeoutDLX() {
        return new DirectExchange(TASK_TIMEOUT_DLX);
    }

    @Bean
    public Queue taskTimeoutQueue() {
        return QueueBuilder.durable(TASK_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding taskTimeoutBinding() {
        return BindingBuilder.bind(taskTimeoutQueue()).to(taskTimeoutDLX()).with(TASK_TIMEOUT_ROUTING_KEY);
    }

    @Bean
    public Queue taskTimeoutDelayQueue() {
        return QueueBuilder.durable(TASK_TIMEOUT_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", TASK_TIMEOUT_DLX)
                .withArgument("x-dead-letter-routing-key", TASK_TIMEOUT_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange taskReminderDLX() {
        return new DirectExchange(TASK_REMINDER_DLX);
    }

    @Bean
    public Queue taskReminderQueue() {
        return QueueBuilder.durable(TASK_REMINDER_QUEUE).build();
    }

    @Bean
    public Binding taskReminderBinding() {
        return BindingBuilder.bind(taskReminderQueue()).to(taskReminderDLX()).with(TASK_REMINDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange grabReleaseDLX() {
        return new DirectExchange(GRAB_RELEASE_DLX);
    }

    @Bean
    public Queue grabReleaseQueue() {
        return QueueBuilder.durable(GRAB_RELEASE_QUEUE).build();
    }

    @Bean
    public Binding grabReleaseBinding() {
        return BindingBuilder.bind(grabReleaseQueue()).to(grabReleaseDLX()).with(GRAB_RELEASE_ROUTING_KEY);
    }

    @Bean
    public Queue grabReleaseDelayQueue() {
        return QueueBuilder.durable(GRAB_RELEASE_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", GRAB_RELEASE_DLX)
                .withArgument("x-dead-letter-routing-key", GRAB_RELEASE_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue aiAnalysisQueue() {
        return QueueBuilder.durable(AI_ANALYSIS_QUEUE).build();
    }

    @Bean
    public DirectExchange aiAnalysisExchange() {
        return new DirectExchange(AI_ANALYSIS_EXCHANGE);
    }

    @Bean
    public Binding aiAnalysisBinding() {
        return BindingBuilder.bind(aiAnalysisQueue()).to(aiAnalysisExchange()).with(AI_ANALYSIS_ROUTING_KEY);
    }
}
