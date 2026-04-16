package com.example.ccc.ai;

import com.alibaba.fastjson2.JSON;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.entity.UserHistoryVO;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.repository.UserRepository;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DatabaseTools {

    private final TaskRepository taskRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public DatabaseTools(TaskRepository taskRepository,
                         TaskSubmissionRepository submissionRepository,
                         UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Tool("根据任务ID查询任务详情。入参: taskId(数字)。返回JSON。")
    public String getTaskById(long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        return JSON.toJSONString(task);
    }

    @Tool("按条件查询任务列表（最多返回limit条）。入参: status(可空), priority(可空), limit(默认10)。返回JSON数组。")
    public String listTasks(Integer status, Integer priority, Integer limit) {
        int size = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        List<Task> tasks = taskRepository
                .findByCondition(null, status, priority, null, PageRequest.of(0, size))
                .getContent();
        return JSON.toJSONString(tasks);
    }

    @Tool("查询某个任务的提交记录。入参: taskId(数字)。返回JSON数组。")
    public String listSubmissionsByTaskId(long taskId) {
        List<TaskSubmission> list = submissionRepository.findByTaskId(taskId);
        return JSON.toJSONString(list);
    }

    @Tool("查询某个用户的历史提交记录（包含任务标题）。入参: userId(数字)。返回JSON数组。")
    public String listUserHistory(long userId) {
        List<UserHistoryVO> list = submissionRepository.findHistoryByUserId(userId);
        return JSON.toJSONString(list);
    }

    @Tool("""
            查询某个任务的评分统计（用于对数据总结/对比）。
            入参: taskId(数字)。
            返回JSON: {taskId, totalSubmissions, gradedCount, avgScore, minScore, maxScore}
            """)
    public String getTaskScoreStats(long taskId) {
        long total = submissionRepository.countByTaskId(taskId);
        TaskSubmissionRepository.TaskScoreStatsProjection stats =
                submissionRepository.getTaskScoreStats(taskId);

        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("taskId", taskId);
        ret.put("totalSubmissions", total);
        ret.put("gradedCount", stats == null ? 0 : stats.getGradedCount());
        ret.put("avgScore", stats == null ? null : stats.getAvgScore());
        ret.put("minScore", stats == null ? null : stats.getMinScore());
        ret.put("maxScore", stats == null ? null : stats.getMaxScore());
        return JSON.toJSONString(ret);
    }

    @Tool("""
            查询某个任务最低分的学生与提交信息（只看已评分提交）。
            入参: taskId(数字)。
            返回JSON: {taskId, user:{id,username,nickname}, score, feedback, submitTime, submissionId}
            """)
    public String getTaskLowestScore(long taskId) {
        TaskSubmission sub = submissionRepository.findTopByTaskIdAndScoreIsNotNullOrderByScoreAscSubmitTimeAsc(taskId);
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("taskId", taskId);
        if (sub == null) {
            ret.put("error", "该任务暂无已评分提交");
            return JSON.toJSONString(ret);
        }

        User u = userRepository.findById(sub.getUserId()).orElse(null);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", sub.getUserId());
        user.put("username", u == null ? null : u.getUsername());
        user.put("nickname", u == null ? null : u.getNickname());

        ret.put("user", user);
        ret.put("score", sub.getScore());
        ret.put("feedback", sub.getFeedback());
        ret.put("submitTime", sub.getSubmitTime());
        ret.put("submissionId", sub.getId());
        return JSON.toJSONString(ret);
    }

    @Tool("""
            查询某个任务最高分的学生与提交信息（只看已评分提交）。
            入参: taskId(数字)。
            返回JSON: {taskId, user:{id,username,nickname}, score, feedback, submitTime, submissionId}
            """)
    public String getTaskHighestScore(long taskId) {
        TaskSubmission sub = submissionRepository.findTopByTaskIdAndScoreIsNotNullOrderByScoreDescSubmitTimeAsc(taskId);
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("taskId", taskId);
        if (sub == null) {
            ret.put("error", "该任务暂无已评分提交");
            return JSON.toJSONString(ret);
        }

        User u = userRepository.findById(sub.getUserId()).orElse(null);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", sub.getUserId());
        user.put("username", u == null ? null : u.getUsername());
        user.put("nickname", u == null ? null : u.getNickname());

        ret.put("user", user);
        ret.put("score", sub.getScore());
        ret.put("feedback", sub.getFeedback());
        ret.put("submitTime", sub.getSubmitTime());
        ret.put("submissionId", sub.getId());
        return JSON.toJSONString(ret);
    }

    @Tool("""
            获取某学生的所有任务评语与得分（用于智能助手做综合评价总结）。
            入参: userId(数字), limit(默认50，最多200)。
            返回JSON数组: [{taskId, taskTitle, score, feedback, submitTime, submissionId}]
            """)
    public String getUserFeedbackCorpus(long userId, Integer limit) {
        int size = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
        List<UserHistoryVO> history = submissionRepository.findHistoryByUserId(userId);

        List<Map<String, Object>> list = history.stream()
                .filter(Objects::nonNull)
                .filter(h -> h.getFeedback() != null && !h.getFeedback().isBlank())
                .limit(size)
                .map(h -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("taskId", h.getTaskId());
                    row.put("taskTitle", h.getTaskTitle());
                    row.put("score", h.getScore());
                    row.put("feedback", h.getFeedback());
                    row.put("submitTime", h.getSubmitTime());
                    row.put("submissionId", h.getSubmissionId());
                    return row;
                })
                .collect(Collectors.toList());

        return JSON.toJSONString(list);
    }

    @Tool("""
            根据用户名获取该学生的整体成绩统计（所有已评分任务的平均分与评分次数）。
            入参: username(字符串)。
            返回JSON: {userId, username, nickname, avgScore, gradedCount}
            """)
    public String getUserScoreStatsByUsername(String username) {
        Map<String, Object> ret = new LinkedHashMap<>();
        if (username == null || username.isBlank()) {
            ret.put("error", "username不能为空");
            return JSON.toJSONString(ret);
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            ret.put("error", "未找到该用户名对应的用户");
            return JSON.toJSONString(ret);
        }
        TaskSubmissionRepository.UserScoreStatsProjection stats =
                submissionRepository.getUserScoreStats(user.getId());
        ret.put("userId", user.getId());
        ret.put("username", user.getUsername());
        ret.put("nickname", user.getNickname());
        ret.put("avgScore", stats == null ? null : stats.getAvgScore());
        ret.put("gradedCount", stats == null ? 0L : stats.getGradedCount());
        return JSON.toJSONString(ret);
    }

    @Tool("""
            根据用户名获取该学生的任务评语语料（用于综合评价总结）。
            入参: username(字符串), limit(默认50，最多200)。
            返回JSON数组: [{taskId, taskTitle, score, feedback, submitTime, submissionId}]
            """)
    public String getUserFeedbackCorpusByUsername(String username, Integer limit) {
        Map<String, Object> err = new LinkedHashMap<>();
        if (username == null || username.isBlank()) {
            err.put("error", "username不能为空");
            return JSON.toJSONString(err);
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            err.put("error", "未找到该用户名对应的用户");
            return JSON.toJSONString(err);
        }
        // 直接复用基于 userId 的实现
        return getUserFeedbackCorpus(user.getId(), limit);
    }

    @Tool("查询某个任务下未提交的学生列表。入参: taskId(数字)。返回JSON数组。")
    public String listUnsubmittedUsers(long taskId) {
        List<User> list = userRepository.findUnsubmittedUsers(taskId);
        return JSON.toJSONString(list);
    }

    @Tool("按用户名关键字模糊查询用户（最多返回limit条）。入参: keyword(字符串), limit(默认10)。返回JSON数组。")
    public String searchUsersByUsername(String keyword, Integer limit) {
        int size = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        if (keyword == null || keyword.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "keyword不能为空");
            return JSON.toJSONString(err);
        }
        List<User> list = userRepository.findByUsernameContaining(keyword);
        if (list.size() > size) {
            list = list.subList(0, size);
        }
        return JSON.toJSONString(list);
    }
}

