package com.example.ccc.repository;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.UserHistoryVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

    // 1. 根据任务ID查询列表
    List<TaskSubmission> findByTaskId(Long taskId);

    // 1.1 查询某任务最低分（只看已评分的提交）
    TaskSubmission findTopByTaskIdAndScoreIsNotNullOrderByScoreAscSubmitTimeAsc(Long taskId);

    // 1.2 查询某任务最高分（只看已评分的提交）
    TaskSubmission findTopByTaskIdAndScoreIsNotNullOrderByScoreDescSubmitTimeAsc(Long taskId);

    interface TaskScoreStatsProjection {
        long getGradedCount();
        Double getAvgScore();
        Integer getMinScore();
        Integer getMaxScore();
    }

    interface UserScoreStatsProjection {
        Double getAvgScore();
        Long getGradedCount();
    }

    // 1.3 任务评分统计（只统计已评分的提交）
    @Query("""
            SELECT COUNT(s) as gradedCount,
                   AVG(s.score) as avgScore,
                   MIN(s.score) as minScore,
                   MAX(s.score) as maxScore
            FROM TaskSubmission s
            WHERE s.taskId = :taskId AND s.score IS NOT NULL
            """)
    TaskScoreStatsProjection getTaskScoreStats(@Param("taskId") Long taskId);

    // 1.5 某个学生的整体成绩统计（所有已评分任务的平均分与评分次数）
    @Query("""
            SELECT AVG(s.score) as avgScore,
                   COUNT(s) as gradedCount
            FROM TaskSubmission s
            WHERE s.userId = :userId AND s.score IS NOT NULL
            """)
    UserScoreStatsProjection getUserScoreStats(@Param("userId") Long userId);

    // 1.4 任务提交总数（含未评分）
    long countByTaskId(Long taskId);

    // 2. 复杂的历史查询 (替代 Mapper 的 selectUserHistory)
    // 注意：UserHistoryVO 必须有全参构造函数
    @Query("SELECT new com.example.ccc.entity.UserHistoryVO(" +
            "s.id, s.fileUrl, s.fileName, s.score, s.feedback, s.submitTime, t.title, t.id) " +
            "FROM TaskSubmission s " +
            "LEFT JOIN Task t ON s.taskId = t.id " +
            "WHERE s.userId = :userId " +
            "ORDER BY s.submitTime DESC")
    List<UserHistoryVO> findHistoryByUserId(@Param("userId") Long userId);

    Page<TaskSubmission> findByIsExcellentTrueOrderBySubmitTimeDesc(Pageable pageable);
}
