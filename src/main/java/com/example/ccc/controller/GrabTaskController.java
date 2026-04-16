package com.example.ccc.controller;

import com.example.ccc.common.Result;
import com.example.ccc.entity.GrabResultVO;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.service.GrabTaskService;
import com.example.ccc.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class GrabTaskController {

    @Autowired
    private GrabTaskService grabTaskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/{taskId}/grab")
    public Result<GrabResultVO> grabTask(@PathVariable Long taskId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        GrabResultVO result = grabTaskService.grabTask(taskId, userId);
        return Result.success(result);
    }

    @GetMapping("/{taskId}/slots")
    public Result<Integer> getAvailableSlots(@PathVariable Long taskId) {
        Integer slots = grabTaskService.getAvailableSlots(taskId);
        if (slots == null) {
            return Result.error(404, "任务不存在或不是限量任务");
        }
        return Result.success(slots);
    }

    @PostMapping("/{taskId}/limited")
    public Result<String> createLimitedTask(
            @PathVariable Long taskId,
            @RequestParam Integer totalSlots,
            @RequestParam Integer rewardPoints) {

        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null || !"PUBLISHER".equals(currentUser.getRole())) {
            return Result.error(403, "权限不足：只有发布者才能创建限量任务");
        }

        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }

        task.setIsLimited(true);
        task.setTotalSlots(totalSlots);
        task.setAvailableSlots(totalSlots);
        task.setRewardPoints(rewardPoints);
        taskRepository.save(task);

        grabTaskService.initSlots(taskId, totalSlots);

        return Result.success("限量任务创建成功，总名额：" + totalSlots);
    }

    @PostMapping("/{taskId}/reset-slots")
    public Result<String> resetTaskSlots(@PathVariable Long taskId) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null || !"PUBLISHER".equals(currentUser.getRole())) {
            return Result.error(403, "权限不足");
        }

        grabTaskService.resetTaskSlots(taskId);
        return Result.success("名额已重置");
    }
}
