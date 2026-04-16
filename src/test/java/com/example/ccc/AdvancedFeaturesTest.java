package com.example.ccc;
import com.example.ccc.common.Result;
import com.example.ccc.controller.SubmissionController;
import com.example.ccc.controller.TaskController;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskRepository;           // ✅ 改为 Repository
import com.example.ccc.repository.TaskSubmissionRepository; // ✅ 改为 Repository
import com.example.ccc.repository.UserRepository;           // ✅ 改为 Repository
import com.example.ccc.service.SubmissionService;
import com.example.ccc.service.TaskService;
import com.example.ccc.utils.UserContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class AdvancedFeaturesTest {

    @Autowired
    private TaskController taskController;
    @Autowired
    private SubmissionController submissionController;
    @Autowired
    private TaskService taskService;
    @Autowired
    private SubmissionService submissionService;

    // ✅ 这里全部注入 Repository
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskSubmissionRepository submissionRepository;

    // 辅助方法：创建用户 (JPA版)
    private Long createUser(String name, String role) {
        User u = userRepository.findByUsername(name);
        if (u == null) {
            u = new User();
            u.setUsername(name);
            u.setNickname(name);
            u.setPassword("123"); // 实际应该加密，测试简化
            u.setRole(role);
            userRepository.save(u); // JPA save
        } else {
            u.setRole(role);
            userRepository.save(u); // JPA save 更新
        }
        return u.getId();
    }

    @Test
    @Transactional
        // 测试完回滚
    void testAllAdvancedFeatures() {
        System.out.println("========== 开始高级功能测试 (JPA版) ==========");

        // 1. 准备数据
        Long adminId = createUser("adv_admin", "PUBLISHER");
        Long userId = createUser("adv_user", "USER");

        // --- 测试功能一：截止日期提醒 ---
        System.out.println(">>> 测试 1: 截止日期提醒");
        UserContext.setUserId(adminId);

        // 创建一个紧急任务 (3小时后截止)
        Task urgentTask = new Task();
        urgentTask.setUserId(adminId);
        urgentTask.setTitle("紧急任务");
        urgentTask.setDueDate(LocalDateTime.now().plusHours(3));
        taskRepository.save(urgentTask); // JPA save

        // 创建一个不紧急任务 (3天后截止)
        Task normalTask = new Task();
        normalTask.setUserId(adminId);
        normalTask.setTitle("普通任务");
        normalTask.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(normalTask); // JPA save

        // 切换到学生视角
        UserContext.setUserId(userId);

        // 调用 Controller 接口
        Result<List<Task>> urgentResult = taskController.getUrgentTasks();
        List<Task> list = urgentResult.getData();

        System.out.println("查询到的紧急任务数: " + list.size());
        // 应该只有1个，因为另一个还没到期
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("紧急任务", list.get(0).getTitle());
        System.out.println("✅ 截止日期逻辑通过");


        // --- 测试功能二：多维度评分 (AOP) ---
        System.out.println(">>> 测试 2: AOP 多维评分");

        // 学生先提交紧急任务
        MockMultipartFile file = new MockMultipartFile("file", "test.doc", "text/plain", "abc".getBytes());
        submissionService.submitFile(urgentTask.getId(), userId, file);

        // 获取提交记录 (JPA findByTaskId)
        List<TaskSubmission> subs = submissionRepository.findByTaskId(urgentTask.getId());
        Assertions.assertFalse(subs.isEmpty());
        Long subId = subs.get(0).getId();

        // 切换回老师打分
        UserContext.setUserId(adminId);

        // 构造多维分数参数 (注意：不传 score，只传 details)
        Map<String, Object> gradeBody = new HashMap<>();
        gradeBody.put("feedback", "AOP测试");

        Map<String, Integer> details = new HashMap<>();
        details.put("quality", 90);
        details.put("creativity", 80); // 平均分应该是 85
        gradeBody.put("details", details);

        // 调用 Controller (触发 AOP)
        submissionController.gradeSubmission(subId, gradeBody);

        // 验证数据库 (JPA findById)
        TaskSubmission updatedSub = submissionRepository.findById(subId).orElseThrow();

        System.out.println("数据库存入的总分: " + updatedSub.getScore());
        System.out.println("数据库存入的详情: " + updatedSub.getDetailScores());

        Assertions.assertEquals(85, updatedSub.getScore()); // 验证 AOP 算分
        Assertions.assertNotNull(updatedSub.getDetailScores());
        System.out.println("✅ AOP 算分逻辑通过");


        // --- 测试功能三：个人仪表盘 (Bean) ---
        System.out.println(">>> 测试 3: 仪表盘统计");
        UserContext.setUserId(userId); // 切换回学生

        Result<Map<String, Double>> dashResult = submissionController.getDashboard();
        Map<String, Double> radar = dashResult.getData();

        System.out.println("仪表盘数据: " + radar);
        // 验证 Bean 是否计算出了数据
        Assertions.assertTrue(radar.containsKey("勤奋度"));
        Assertions.assertTrue(radar.containsKey("作业质量"));

        System.out.println("✅ 仪表盘 Bean 统计通过");

        System.out.println("========== 全部测试成功 ==========");
    }
}
