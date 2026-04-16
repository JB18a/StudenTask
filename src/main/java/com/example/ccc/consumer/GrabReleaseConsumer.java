package com.example.ccc.consumer;

import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.GrabReleaseMessage;
import com.example.ccc.service.GrabTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GrabReleaseConsumer {

    private static final Logger logger = LoggerFactory.getLogger(GrabReleaseConsumer.class);

    @Autowired
    private GrabTaskService grabTaskService;

    @RabbitListener(queues = RabbitMQConfig.GRAB_RELEASE_QUEUE)
    public void handleGrabRelease(GrabReleaseMessage message) {
        logger.debug("Received grab release message: taskId={}, userId={}", 
                message.getTaskId(), message.getUserId());

        try {
            grabTaskService.releaseSlot(message.getTaskId(), message.getUserId());
        } catch (Exception e) {
            logger.error("Failed to process grab release: taskId={}, userId={}, error={}",
                    message.getTaskId(), message.getUserId(), e.getMessage());
        }
    }
}
