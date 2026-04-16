package com.example.ccc.entity;

import java.io.Serializable;

public class GrabTaskMessage implements Serializable {

    private Long taskId;
    private Long userId;
    private Integer remainingSlots;
    private Long timestamp;

    public GrabTaskMessage() {}

    public GrabTaskMessage(Long taskId, Long userId, Integer remainingSlots) {
        this.taskId = taskId;
        this.userId = userId;
        this.remainingSlots = remainingSlots;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getRemainingSlots() { return remainingSlots; }
    public void setRemainingSlots(Integer remainingSlots) { this.remainingSlots = remainingSlots; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
