package com.example.ccc.entity;
import jakarta.persistence.*; // 引入 JPA
import java.time.LocalDateTime;

@Entity // 必须加
@Table(name = "task_submission") // 必须加
public class TaskSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    private Integer score;
    private String feedback;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    // JPA 需要空构造函数
    public TaskSubmission() {}

    @PrePersist
    protected void onCreate() {
        submitTime = LocalDateTime.now();
    }

    @Column(name = "detail_scores", columnDefinition = "TEXT")
    private String detailScores;

    @Column(name = "is_excellent")
    private Boolean isExcellent = false;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_suggestion", columnDefinition = "TEXT")
    private String aiSuggestion;

    @Column(name = "ai_plagiarism_score")
    private Double aiPlagiarismScore;

    @Column(name = "ai_plagiarism_details", columnDefinition = "TEXT")
    private String aiPlagiarismDetails;

    @Column(name = "ai_analysis_status")
    private String aiAnalysisStatus;

    @Column(name = "ai_analysis_time")
    private LocalDateTime aiAnalysisTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }

    public String getDetailScores() { return detailScores; }
    public void setDetailScores(String detailScores) { this.detailScores = detailScores; }

    public Boolean getIsExcellent() { return isExcellent; }
    public void setIsExcellent(Boolean isExcellent) { this.isExcellent = isExcellent; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }

    public Double getAiPlagiarismScore() { return aiPlagiarismScore; }
    public void setAiPlagiarismScore(Double aiPlagiarismScore) { this.aiPlagiarismScore = aiPlagiarismScore; }

    public String getAiPlagiarismDetails() { return aiPlagiarismDetails; }
    public void setAiPlagiarismDetails(String aiPlagiarismDetails) { this.aiPlagiarismDetails = aiPlagiarismDetails; }

    public String getAiAnalysisStatus() { return aiAnalysisStatus; }
    public void setAiAnalysisStatus(String aiAnalysisStatus) { this.aiAnalysisStatus = aiAnalysisStatus; }

    public LocalDateTime getAiAnalysisTime() { return aiAnalysisTime; }
    public void setAiAnalysisTime(LocalDateTime aiAnalysisTime) { this.aiAnalysisTime = aiAnalysisTime; }
}
