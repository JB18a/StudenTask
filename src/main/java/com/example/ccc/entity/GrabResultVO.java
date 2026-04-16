package com.example.ccc.entity;

public class GrabResultVO {

    private Boolean success;
    private String message;
    private Long taskId;
    private Integer availableSlots;
    private Integer rewardPoints;

    public GrabResultVO() {}

    public GrabResultVO(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static GrabResultVO success(Long taskId, Integer availableSlots, Integer rewardPoints) {
        GrabResultVO vo = new GrabResultVO(true, "抢单成功");
        vo.setTaskId(taskId);
        vo.setAvailableSlots(availableSlots);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    public static GrabResultVO fail(String message) {
        return new GrabResultVO(false, message);
    }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Integer getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(Integer availableSlots) { this.availableSlots = availableSlots; }

    public Integer getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(Integer rewardPoints) { this.rewardPoints = rewardPoints; }
}
