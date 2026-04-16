package com.example.ccc.service;

import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskTimeoutMessage;
import com.example.ccc.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class TaskTimeoutService {

    private static final Logger logger = LoggerFactory.getLogger(TaskTimeoutService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TaskRepository taskRepository;

    public void scheduleTaskTimeout(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null || task.getDueDate() == null) {
            logger.warn("Task {} not found or has no due date", taskId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = task.getDueDate();

        if (dueDate.isBefore(now)) {
            logger.debug("Task {} already expired, skip scheduling", taskId);
            return;
        }

        long delayMillis = Duration.between(now, dueDate).toMillis();

        TaskTimeoutMessage message = new TaskTimeoutMessage(taskId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_TIMEOUT_DELAY_QUEUE,
                message,
                msg -> {
                    msg.getMessageProperties().setDelay((int) delayMillis);
                    return msg;
                }
        );

        logger.debug("Scheduled timeout for task {} in {} ms", taskId, delayMillis);
    }

    public void scheduleTaskTimeout(Long taskId, long delaySeconds) {
        TaskTimeoutMessage message = new TaskTimeoutMessage(taskId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_TIMEOUT_DELAY_QUEUE,
                message,
                msg -> {
                    msg.getMessageProperties().setDelay((int) TimeUnit.SECONDS.toMillis(delaySeconds));
                    return msg;
                }
        );

        logger.debug("Scheduled timeout for task {} in {} seconds", taskId, delaySeconds);
    }

    public void cancelTaskTimeout(Long taskId) {
        logger.debug("Timeout cancellation requested for task {} (note: DLX messages cannot be easily cancelled)", taskId);
    }
}
