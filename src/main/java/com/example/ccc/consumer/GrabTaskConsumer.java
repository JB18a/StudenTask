package com.example.ccc.consumer;

import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.GrabTaskMessage;
import com.example.ccc.entity.Task;
import com.example.ccc.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GrabTaskConsumer {

    private static final Logger logger = LoggerFactory.getLogger(GrabTaskConsumer.class);

    @Autowired
    private TaskRepository taskRepository;

    @RabbitListener(queues = RabbitMQConfig.GRAB_TASK_QUEUE)
    public void handleGrabTaskMessage(GrabTaskMessage message) {
        logger.debug("Received grab task message: taskId={}, userId={}, remainingSlots={}",
                message.getTaskId(), message.getUserId(), message.getRemainingSlots());

        try {
            Task task = taskRepository.findById(message.getTaskId()).orElse(null);
            if (task != null) {
                task.setAvailableSlots(message.getRemainingSlots());
                taskRepository.save(task);
                logger.debug("Updated task {} available slots to {}",
                        message.getTaskId(), message.getRemainingSlots());
            }
        } catch (Exception e) {
            logger.error("Failed to process grab task message: {}", e.getMessage());
        }
    }
}
