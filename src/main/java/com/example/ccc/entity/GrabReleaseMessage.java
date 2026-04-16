package com.example.ccc.entity;

import java.io.Serializable;

public class GrabReleaseMessage implements Serializable {

    private Long taskId;
    private Long userId;
    private Long grabTime;
    private Long expireTime;

    public GrabReleaseMessage() {}

    public GrabReleaseMessage(Long taskId, Long userId) {
        this.taskId = taskId;
        this.userId = userId;
        this.grabTime = System.currentTimeMillis();
        this.expireTime = this.grabTime + 30 * 60 * 1000;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getGrabTime() { return grabTime; }
    public void setGrabTime(Long grabTime) { this.grabTime = grabTime; }

    public Long getExpireTime() { return expireTime; }
    public void setExpireTime(Long expireTime) { this.expireTime = expireTime; }
}
