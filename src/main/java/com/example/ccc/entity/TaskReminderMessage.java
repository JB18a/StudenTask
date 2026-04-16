package com.example.ccc.entity;

import java.io.Serializable;

public class TaskReminderMessage implements Serializable {

    private Long taskId;
    private String taskTitle;
    private Long delayMillis;

    public TaskReminderMessage() {}

    public TaskReminderMessage(Long taskId, String taskTitle, Long delayMillis) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.delayMillis = delayMillis;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public Long getDelayMillis() { return delayMillis; }
    public void setDelayMillis(Long delayMillis) { this.delayMillis = delayMillis; }
}
