package com.example.ccc.controller;
import com.example.ccc.common.Result;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission; // ✅ 新增引用
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.service.SubmissionService; // ✅ 新增引用
import com.example.ccc.service.TaskService;
import com.example.ccc.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List; // ✅ 新增引用
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    // ✅ 新增：注入 SubmissionService 以便查询提交记录
    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private com.example.ccc.repository.UserRepository userRepository; // ✅ 注入 JPA Repository

    /**
     * 获取任务列表
     */
    @GetMapping
    public Result<Page<Task>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer priority) {

        Long currentUserId = UserContext.getUserId();

        // 1. 查询当前用户角色 (JPA 写法)
        // findById 返回的是 Optional，所以要用 .orElse(null)
        User user = userRepository.findById(currentUserId).orElse(null);

        boolean isStudent = true; // 默认视为学生（需要过滤）
        if (user != null && "PUBLISHER".equals(user.getRole())) {
            isStudent = false; // 是发布者（不需要过滤）
        }

        // 2. 设定查询逻辑
        // creatorId 传 null，表示查询公共任务池
        // 传 currentUserId 和 isStudent 进去，让 Service 决定是否过滤
        Page<Task> pageResult = taskService.getTaskList(pageNum, pageSize, null, status, priority, currentUserId, isStudent);

        return Result.success(pageResult);
    }

    /**
     * 发布新任务
     */
    @PostMapping
    public Result createTask(@RequestBody Task task) {
        Long userId = UserContext.getUserId();
        task.setUserId(userId);
        // 这里 taskService.createTask 现主要负责保存任务基本信息
        taskService.createTask(task, null);
        return Result.success("任务发布成功");
    }

    /**
     * 获取单个任务详情
     * 修改：允许任何人查看任务详情（为了让学生能交作业）
     */
    @GetMapping("/{id}")
    public Result<Task> detail(@PathVariable Long id) {
        // 调用 Service 新加的方法，只按 ID 查，不卡用户
        Task task = taskService.getTaskById(id);

        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(task);
    }

    /**
     * ✅ 新增接口：查看某个任务的所有提交记录
     * 场景：发布者进入“作业批改”页面时调用
     * URL: GET /api/tasks/{id}/submissions
     */
    @GetMapping("/{id}/submissions")
    public Result<List<TaskSubmission>> viewSubmissions(@PathVariable Long id) {
        // 这里可以加权限校验：确保当前用户是该任务的发布者
        // Long userId = UserContext.getUserId();
        // ... 校验逻辑 ...

        List<TaskSubmission> list = submissionService.getSubmissionsForTask(id);
        return Result.success(list);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        task.setUserId(UserContext.getUserId());
        taskService.updateTask(task);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    public Result updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        taskService.updateTaskStatus(id, params.get("status"), UserContext.getUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        taskService.deleteTask(id, UserContext.getUserId());
        return Result.success();
    }

    /**
     * 获取即将过期的任务 (只提醒未完成的)
     */
    @GetMapping("/urgent")
    public Result<List<Task>> getUrgentTasks() {
        Long userId = UserContext.getUserId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24h = now.plusHours(24);

        // 现在这里传了 3 个参数，Repository 也接收 3 个参数，不会再报错了
        List<Task> urgentList = taskRepository.findUrgentTasks(userId, now, next24h);

        return Result.success(urgentList);
    }
}