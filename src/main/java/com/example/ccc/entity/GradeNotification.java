package com.example.ccc.entity;

import java.time.LocalDateTime;

public class GradeNotification {
    
    private Long userId;
    private Long submissionId;
    private Long taskId;
    private String taskTitle;
    private String fileName;
    private String fileUrl;
    private Integer score;
    private String feedback;
    private String detailScores;
    private LocalDateTime submitTime;
    private LocalDateTime gradedTime;
    private String type;

    public GradeNotification() {
        this.gradedTime = LocalDateTime.now();
        this.type = "GRADE_UPDATE";
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getDetailScores() {
        return detailScores;
    }

    public void setDetailScores(String detailScores) {
        this.detailScores = detailScores;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getGradedTime() {
        return gradedTime;
    }

    public void setGradedTime(LocalDateTime gradedTime) {
        this.gradedTime = gradedTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
