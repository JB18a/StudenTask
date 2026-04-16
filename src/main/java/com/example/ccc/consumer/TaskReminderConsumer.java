package com.example.ccc.consumer;

import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskReminderMessage;
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskReminderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TaskReminderConsumer.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @RabbitListener(queues = RabbitMQConfig.TASK_REMINDER_QUEUE)
    public void handleReminder(TaskReminderMessage message) {
        logger.debug("Received task reminder: taskId={}", message.getTaskId());

        try {
            Task task = taskRepository.findById(message.getTaskId()).orElse(null);
            if (task == null || task.getStatus() != null && task.getStatus() == 1) {
                logger.debug("Task {} already completed, skip reminder", message.getTaskId());
                return;
            }

            if (task.getStatus() != null && task.getStatus() == 2) {
                logger.debug("Task {} already expired, skip reminder", message.getTaskId());
                return;
            }

            List<User> unsubmittedUsers = userRepository.findUnsubmittedUsers(task.getId());
            if (unsubmittedUsers.isEmpty()) {
                logger.debug("All users have submitted task {}", message.getTaskId());
                return;
            }

            logger.info("Sending reminders to {} users for task: {} (ID: {})",
                    unsubmittedUsers.size(), task.getTitle(), message.getTaskId());

            for (User user : unsubmittedUsers) {
                sendNotification(user, task, message.getDelayMillis());
            }

        } catch (Exception e) {
            logger.error("Failed to process task reminder: {}", e.getMessage());
        }
    }

    private void sendNotification(User user, Task task, Long delayMillis) {
        logger.info("========================================");
        logger.info("提醒学生: {} (ID: {})", user.getNickname() != null ? user.getNickname() : user.getUsername(), user.getId());
        logger.info("任务: {} (ID: {})", task.getTitle(), task.getId());
        logger.info("距离截止还有 {} 小时", delayMillis / 3600000);
        logger.info("========================================");
    }
}
