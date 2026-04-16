package com.example.ccc.entity;
import java.time.LocalDateTime;

// 不用 @Data，手动写
public class UserHistoryVO {
    private Long submissionId;
    private String fileUrl;
    private String fileName;
    private Integer score;
    private String feedback;
    private LocalDateTime submitTime;
    private String taskTitle;
    private Long taskId;

    // --- 1. 必须有：空构造函数 ---
    public UserHistoryVO() {}

    // --- 2. 【核心】必须有：全参构造函数 (用于 JPA @Query) ---
    // 参数顺序必须和 TaskSubmissionRepository 里的 @Query 语句一致！
    public UserHistoryVO(Long submissionId, String fileUrl, String fileName,
                         Integer score, String feedback, LocalDateTime submitTime,
                         String taskTitle, Long taskId) {
        this.submissionId = submissionId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.score = score;
        this.feedback = feedback;
        this.submitTime = submitTime;
        this.taskTitle = taskTitle;
        this.taskId = taskId;
    }

    // --- 3. Getter / Setter (省略，请自行生成或用 IDEA 生成) ---
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    // ... 其他 Get/Set ...
    public String getTaskTitle() { return taskTitle; }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}
