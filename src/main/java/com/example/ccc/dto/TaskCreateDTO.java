package com.example.ccc.dto;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class TaskCreateDTO {

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;

    @Future(message = "截止日期必须是将来时间")
    private LocalDateTime dueDate;

    private Integer priority;

    // --- Getter / Setter ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
