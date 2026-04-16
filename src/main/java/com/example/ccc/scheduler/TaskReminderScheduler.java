package com.example.ccc.scheduler;

import com.example.ccc.entity.Task;
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TaskReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TaskReminderScheduler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Scheduled(cron = "0 0 8,12,18 * * ?")
    public void checkExpiringTasks() {
        logger.info("========== 开始执行任务过期提醒定时任务 ==========");
        logger.info("执行时间: {}", LocalDateTime.now().format(formatter));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24h = now.plusHours(24);

        List<Task> expiringTasks = taskRepository.findExpiringTasks(now, next24h);

        if (expiringTasks.isEmpty()) {
            logger.info("没有即将过期的任务");
            return;
        }

        logger.info("发现 {} 个即将在24小时内过期的任务", expiringTasks.size());

        int totalReminders = 0;

        for (Task task : expiringTasks) {
            List<User> unsubmittedUsers = userRepository.findUnsubmittedUsers(task.getId());

            if (!unsubmittedUsers.isEmpty()) {
                logger.info("--------------------------------------------------");
                logger.info("任务: {} (ID: {})", task.getTitle(), task.getId());
                logger.info("截止时间: {}", task.getDueDate().format(formatter));
                logger.info("以下 {} 位学生尚未提交，需要提醒:", unsubmittedUsers.size());

                for (User user : unsubmittedUsers) {
                    logger.info("  - 学生: {} (ID: {})", user.getNickname() != null ? user.getNickname() : user.getUsername(), user.getId());
                    totalReminders++;
                }
            }
        }

        logger.info("========== 定时任务执行完成，共发送 {} 条提醒 ==========", totalReminders);
    }

    @Scheduled(fixedRate = 60000)
    public void checkOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past1h = now.minusHours(1);

        List<Task> overdueTasks = taskRepository.findExpiringTasks(past1h, now);

        if (!overdueTasks.isEmpty()) {
            logger.warn("发现 {} 个任务刚刚过期！", overdueTasks.size());
            for (Task task : overdueTasks) {
                logger.warn("任务过期: {} (截止时间: {})", task.getTitle(), task.getDueDate().format(formatter));
            }
        }
    }
}
