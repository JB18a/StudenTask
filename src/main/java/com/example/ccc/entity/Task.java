package com.example.ccc.entity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*; // 引入 JPA 核心注解
import java.time.LocalDateTime;

@Entity // 标记为 JPA 实体
@Table(name = "task") // 映射数据库表名
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自增
    private Long id;

    @Column(name = "user_id", nullable = false) // 映射数据库 user_id 字段
    private Long userId;

    private String title;

    @Column(columnDefinition = "TEXT") // 描述字段通常较长
    private String description;

    @Column(name = "attachment_url") // 映射 attachment_url
    private String attachmentUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    private Integer priority; // 0-低，1-中，2-高
    private Integer status;   // 0-未完成，1-已完成，2-已逾期

    @Column(name = "is_limited")
    private Boolean isLimited = false;

    @Column(name = "total_slots")
    private Integer totalSlots = 0;

    @Column(name = "available_slots")
    private Integer availableSlots = 0;

    @Column(name = "reward_points")
    private Integer rewardPoints = 0;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "create_time", updatable = false) // 创建时间不可更新
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    // --- JPA 必须要求的无参构造函数 ---
    public Task() {
    }

    // --- 自动填充时间的钩子方法 ---
    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.priority == null) this.priority = 0; // 默认优先级
        if (this.status == null) this.status = 0;     // 默认状态
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    // --- 手动 Getter 和 Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsLimited() { return isLimited; }
    public void setIsLimited(Boolean isLimited) { this.isLimited = isLimited; }

    public Integer getTotalSlots() { return totalSlots; }
    public void setTotalSlots(Integer totalSlots) { this.totalSlots = totalSlots; }

    public Integer getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(Integer availableSlots) { this.availableSlots = availableSlots; }

    public Integer getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(Integer rewardPoints) { this.rewardPoints = rewardPoints; }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
