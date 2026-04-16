package com.example.ccc.entity;

import java.io.Serializable;

public class AiAnalysisMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long taskId;
    private Long userId;
    private String fileUrl;
    private String fileName;

    public AiAnalysisMessage() {}

    public AiAnalysisMessage(Long submissionId, Long taskId, Long userId, String fileUrl, String fileName) {
        this.submissionId = submissionId;
        this.taskId = taskId;
        this.userId = userId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}