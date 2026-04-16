package com.example.ccc.consumer;

import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskTimeoutMessage;
import com.example.ccc.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskTimeoutConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TaskTimeoutConsumer.class);

    @Autowired
    private TaskRepository taskRepository;

    @RabbitListener(queues = RabbitMQConfig.TASK_TIMEOUT_QUEUE)
    public void handleTaskTimeout(TaskTimeoutMessage message) {
        logger.debug("Received task timeout message: taskId={}", message.getTaskId());

        try {
            Task task = taskRepository.findById(message.getTaskId()).orElse(null);
            if (task == null) {
                logger.warn("Task not found: {}", message.getTaskId());
                return;
            }

            if (task.getStatus() != null && task.getStatus() == 1) {
                logger.debug("Task {} already completed, skip timeout handling", message.getTaskId());
                return;
            }

            if (task.getStatus() != null && task.getStatus() == 2) {
                logger.debug("Task {} already expired, skip", message.getTaskId());
                return;
            }

            task.setStatus(2);
            taskRepository.save(task);

            logger.info("Task {} marked as expired due to timeout", message.getTaskId());

        } catch (Exception e) {
            logger.error("Failed to process task timeout: taskId={}, error={}",
                    message.getTaskId(), e.getMessage());
        }
    }
}
