package com.example.ccc.repository;
import com.example.ccc.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 方法 1: 任务列表查询 (支持分页、筛选、排除已完成)
     * 用于 TaskService.getTaskList
     */
    @Query("SELECT t FROM Task t WHERE " +
            "(:creatorId IS NULL OR t.userId = :creatorId) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:priority IS NULL OR t.priority = :priority) " +
            // 如果 excludeUserId 不为空，则排除该用户提交过的任务
            "AND (:excludeUserId IS NULL OR NOT EXISTS " +
            "    (SELECT s FROM TaskSubmission s WHERE s.taskId = t.id AND s.userId = :excludeUserId)) " +
            "ORDER BY t.createTime DESC")
    Page<Task> findByCondition(@Param("creatorId") Long creatorId,
                               @Param("status") Integer status,
                               @Param("priority") Integer priority,
                               @Param("excludeUserId") Long excludeUserId,
                               Pageable pageable);

    /**
     * 方法 2: 紧急任务查询 (用于弹窗提醒)
     * 逻辑：截止时间在 [start, end] 之间，且该用户未提交
     * 用于 TaskController.getUrgentTasks
     */
    @Query("SELECT t FROM Task t WHERE " +
            "t.dueDate BETWEEN :start AND :end " +
            "AND NOT EXISTS (SELECT s FROM TaskSubmission s WHERE s.taskId = t.id AND s.userId = :userId)")
    List<Task> findUrgentTasks(@Param("userId") Long userId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    /**
     * 方法 3: 定时任务专用 - 查询即将过期的任务（未完成且截止时间在未来指定小时内）
     * 用于 TaskReminderScheduler 自动提醒
     */
    @Query("SELECT t FROM Task t WHERE " +
            "t.dueDate BETWEEN :start AND :end " +
            "AND t.status = 0")
    List<Task> findExpiringTasks(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);
}
