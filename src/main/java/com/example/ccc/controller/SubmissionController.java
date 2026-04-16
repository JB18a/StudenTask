package com.example.ccc.controller;

import com.example.ccc.common.Result;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.entity.UserHistoryVO;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.service.LeaderboardService;
import com.example.ccc.service.NotificationService;
import com.example.ccc.service.SubmissionService;
import com.example.ccc.service.AiAsyncService;
import com.example.ccc.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.ccc.annotation.AutoAvgScore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AiAsyncService aiAsyncService;

    // ✅ 注入 JPA Repository
    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private com.example.ccc.component.ScoreCalculator scoreCalculator;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.example.ccc.component.IdempotencyLock idempotencyLock;

    /**
     * 1. 普通用户：上传文件提交任务
     */
    @PostMapping("/task/{taskId}")
    public Result submitTask(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        try {
            submissionService.submitFile(taskId, userId, file);
            return Result.success("文件提交成功");
        } catch (Exception e) {
            return Result.error(500, "提交失败: " + e.getMessage());
        }
    }

    /**
     * 2. 发布者：给某个提交记录打分
     */
    @PostMapping("/{submissionId}/grade")
    @AutoAvgScore
    public Result gradeSubmission(
            @PathVariable Long submissionId,
            @RequestBody Map<String, Object> body) {

        Long currentUserId = UserContext.getUserId();
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        if (currentUser == null || !"PUBLISHER".equals(currentUser.getRole())) {
            return Result.error(403, "权限不足：只有发布者才能评分");
        }
        String detailScores = (String) body.get("detailScores");
        Integer score = (Integer) body.get("score");
        String feedback = (String) body.get("feedback");

        String lockKey = "grade:submission:" + submissionId;

        try {
            idempotencyLock.executeWithLock(lockKey, 500, () -> {
                TaskSubmission submission = submissionRepository.findById(submissionId)
                        .orElseThrow(() -> new RuntimeException("提交记录不存在"));

                submission.setScore(score);
                submission.setFeedback(feedback);
                submission.setDetailScores(detailScores);
                submissionRepository.save(submission);

                leaderboardService.updateScore(submission.getUserId(), score);

                notificationService.sendGradeNotification(submission);
                return null;
            });

            return Result.success("评分成功");
        } catch (RuntimeException e) {
            return Result.error(429, "请求过于频繁，请稍后重试");
        }
    }

    /**
     * 3. 【核心修复】普通用户查看成绩单 + 获取身份信息
     */
    @GetMapping("/my-report")
    public Result<Map<String, Object>> getMyReport() {
        Long currentUserId = UserContext.getUserId();

        // 1. 查询作业历史 (调用 Repository 里写的 @Query 方法)
        List<UserHistoryVO> history = submissionRepository.findHistoryByUserId(currentUserId);

        // 2. 计算平均分
        double avgScore = history.stream()
                .filter(h -> h.getScore() != null)
                .mapToInt(UserHistoryVO::getScore)
                .average()
                .orElse(0.0);

        // 3. 【关键】查询用户信息 (JPA 写法)
        User me = userRepository.findById(currentUserId).orElse(null);

        Map<String, Object> userInfo = new HashMap<>();
        if (me != null) {
            userInfo.put("username", me.getUsername());
            userInfo.put("nickname", me.getNickname());
            // 前端 store/index.js 依赖这个字段来判断权限
            userInfo.put("role", me.getRole());
        }

        // 4. 组装返回
        Map<String, Object> report = new HashMap<>();
        report.put("submissions", history);
        report.put("averageScore", String.format("%.1f", avgScore));
        report.put("totalCount", history.size());
        report.put("userInfo", userInfo);

        return Result.success(report);
    }

    /**
     * 获取个人能力仪表盘数据
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Double>> getDashboard() {
        Long userId = UserContext.getUserId();

        // 1. 查数据
        List<UserHistoryVO> history = submissionRepository.findHistoryByUserId(userId);

        // 2. 用 Bean 算数据
        Map<String, Double> radarData = scoreCalculator.analyze(history);

        return Result.success(radarData);
    }

    /**
     * 4. 【新增】发布者查看某任务的“未提交/缺席”名单
     * URL: GET /api/submissions/{taskId}/unsubmitted
     */
    @GetMapping("/{taskId}/unsubmitted")
    public Result<List<User>> getUnsubmittedList(@PathVariable Long taskId) {
        // 权限检查
        Long currentUserId = UserContext.getUserId();
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null || !"PUBLISHER".equals(currentUser.getRole())) {
            return Result.error(403, "权限不足");
        }

        List<User> list = submissionService.getUnsubmittedUsers(taskId);
        return Result.success(list);
    }

    @PostMapping("/{submissionId}/trigger-ai-analysis")
    public Result triggerAiAnalysis(@PathVariable Long submissionId) {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null || !"PUBLISHER".equals(currentUser.getRole())) {
            return Result.error(403, "权限不足：只有发布者才能触发AI分析");
        }

        TaskSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            return Result.error(404, "提交记录不存在");
        }

        aiAsyncService.triggerAiAnalysisAsync(submissionId);
        return Result.success("AI分析任务已触发，请在稍后刷新查看结果");
    }

    @GetMapping("/{submissionId}/ai-analysis-status")
    public Result<Map<String, Object>> getAiAnalysisStatus(@PathVariable Long submissionId) {
        TaskSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            return Result.error(404, "提交记录不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", submission.getAiAnalysisStatus());
        result.put("summary", submission.getAiSummary());
        result.put("suggestion", submission.getAiSuggestion());
        result.put("plagiarismScore", submission.getAiPlagiarismScore());
        result.put("plagiarismDetails", submission.getAiPlagiarismDetails());
        result.put("analysisTime", submission.getAiAnalysisTime());

        StringBuilder resultText = new StringBuilder();
        if (submission.getAiSummary() != null) {
            resultText.append("📝 作业总结：").append(submission.getAiSummary()).append("\n\n");
        }
        if (submission.getAiSuggestion() != null) {
            resultText.append(submission.getAiSuggestion()).append("\n\n");
        }
        if (submission.getAiPlagiarismScore() != null) {
            resultText.append("🔍 查重分数：").append(submission.getAiPlagiarismScore()).append("%\n");
        }
        if (submission.getAiPlagiarismDetails() != null && !submission.getAiPlagiarismDetails().isEmpty()) {
            resultText.append(submission.getAiPlagiarismDetails());
        }
        result.put("result", resultText.toString());

        return Result.success(result);
    }
}
