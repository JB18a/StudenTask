package com.example.ccc.service;

import com.example.ccc.entity.GradeNotification;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.websocket.GradeNotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final GradeNotificationHandler notificationHandler;
    private final TaskRepository taskRepository;

    @Autowired
    public NotificationService(GradeNotificationHandler notificationHandler, TaskRepository taskRepository) {
        this.notificationHandler = notificationHandler;
        this.taskRepository = taskRepository;
    }

    public void sendGradeNotification(TaskSubmission submission) {
        Task task = taskRepository.findById(submission.getTaskId()).orElse(null);
        String taskTitle = task != null ? task.getTitle() : "未知任务";

        GradeNotification notification = new GradeNotification();
        notification.setUserId(submission.getUserId());
        notification.setSubmissionId(submission.getId());
        notification.setTaskId(submission.getTaskId());
        notification.setTaskTitle(taskTitle);
        notification.setFileName(submission.getFileName());
        notification.setFileUrl(submission.getFileUrl());
        notification.setScore(submission.getScore());
        notification.setFeedback(submission.getFeedback());
        notification.setDetailScores(submission.getDetailScores());
        notification.setSubmitTime(submission.getSubmitTime());

        notificationHandler.sendNotificationToUser(submission.getUserId(), notification);
        
        logger.info("成绩更新通知已发送: userId={}, taskId={}, score={}", 
                submission.getUserId(), submission.getTaskId(), submission.getScore());
    }

    public boolean isUserOnline(Long userId) {
        return notificationHandler.isUserOnline(userId);
    }
}
