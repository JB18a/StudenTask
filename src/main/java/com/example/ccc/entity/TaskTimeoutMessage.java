package com.example.ccc.entity;

import java.io.Serializable;

public class TaskTimeoutMessage implements Serializable {

    private Long taskId;
    private Long timestamp;

    public TaskTimeoutMessage() {}

    public TaskTimeoutMessage(Long taskId) {
        this.taskId = taskId;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
