package com.example.ccc;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.entity.UserHistoryVO;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.service.SubmissionService;
import com.example.ccc.service.TaskService;
import com.example.ccc.utils.UserContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class AdminFlowTest {

    @Autowired
    private UserRepository userRepository; // ✅ 改为 Repository

    @Autowired
    private TaskService taskService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private TaskSubmissionRepository submissionRepository; // ✅ 改为 Repository

    private Long adminId;
    private Long userId;

    /**
     * 辅助方法：保证数据库里一定有这个用户，并返回真实ID (JPA版)
     */
    private Long getOrCreateUser(String username, String nickname, String role) {
        // JPA 查询
        User user = userRepository.findByUsername(username);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setPassword("123456"); // 测试环境直接存明文，反正走 UserContext 不走登录校验
            user.setNickname(nickname);
            user.setRole(role);
            // JPA 保存 (insert)
            userRepository.save(user);
        } else {
            user.setRole(role);
            // JPA 保存 (update)
            userRepository.save(user);
        }
        return user.getId();
    }

    @Test
    void testFullBusinessFlow() throws IOException {
        System.out.println("========== 开始超级全流程测试 (JPA版) ==========");

        // --- 1. 准备账号 ---
        this.adminId = getOrCreateUser("admin_jpa_test", "JPA管理员", "PUBLISHER");
        this.userId = getOrCreateUser("user_jpa_test", "JPA学生", "USER");
        System.out.println(">>> 1. 账号就位: AdminID=" + adminId + ", UserID=" + userId);


        // --- 2. 管理员发布任务 ---
        UserContext.setUserId(adminId);
        Task task = new Task();
        task.setUserId(adminId);
        task.setTitle("期末Java大作业(JPA)");
        task.setDescription("请提交源码");
        task.setDueDate(LocalDateTime.now().plusDays(10));
        // TaskService 已经改造成 JPA 了，可以直接用
        taskService.createTask(task, null);
        Long taskId = task.getId();
        System.out.println(">>> 2. 任务发布成功 ID=" + taskId);


        // --- 3. 普通用户提交文件 ---
        UserContext.setUserId(userId);
        MockMultipartFile file = new MockMultipartFile(
                "file", "作业_jpa.docx", "text/plain", "content".getBytes()
        );
        submissionService.submitFile(taskId, userId, file);
        System.out.println(">>> 3. 用户提交文件成功");


        // --- 4. 管理员打分 (JPA 逻辑变化) ---
        UserContext.setUserId(adminId);

        // 4.1 查列表
        List<TaskSubmission> list = submissionRepository.findByTaskId(taskId);
        Assertions.assertFalse(list.isEmpty(), "提交列表不能为空");

        // 4.2 修改对象
        TaskSubmission sub = list.get(0);
        sub.setScore(98);
        sub.setFeedback("JPA重构完美！");

        // 4.3 保存 (JPA save = update)
        submissionRepository.save(sub);
        System.out.println(">>> 4. 管理员打分完成 (98分)");


        // --- 5. 普通用户查看自己的成绩单 ---
        System.out.println(">>> 5. 普通用户查询自己的成绩单...");
        UserContext.setUserId(userId);

        // 调用 Repository 的自定义 JPQL 方法
        List<UserHistoryVO> myReport = submissionRepository.findHistoryByUserId(userId);

        Assertions.assertFalse(myReport.isEmpty(), "成绩单不应该为空");
        UserHistoryVO record = myReport.get(0);

        System.out.println("----------------------------------");
        System.out.println("查看到的任务: " + record.getTaskTitle());
        System.out.println("查看到的分数: " + record.getScore());
        System.out.println("查看到的评语: " + record.getFeedback());
        System.out.println("----------------------------------");

        Assertions.assertEquals(98, record.getScore());
        Assertions.assertEquals("JPA重构完美！", record.getFeedback());

        System.out.println("========== 全流程测试完美通过！ ==========");
    }
}