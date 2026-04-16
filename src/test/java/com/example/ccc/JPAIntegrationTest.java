package com.example.ccc;
import com.example.ccc.dto.RegisterDTO;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.entity.UserHistoryVO;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.service.SubmissionService;
import com.example.ccc.service.TaskService;
import com.example.ccc.service.UserService;
import com.example.ccc.utils.UserContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class JPAIntegrationTest {

    @Autowired
    private UserService userService; // 测试 DTO 注册

    @Autowired
    private TaskService taskService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskSubmissionRepository submissionRepository; // 测试复杂查询

    /**
     * 测试：全流程 (注册DTO -> 发布 -> 提交 -> 复杂查询)
     * 加 @Transactional 注解，测试跑完会自动回滚，不污染数据库
     */
    @Test
    @Transactional
    void testFullJPAFlow() throws IOException {
        System.out.println("========== 开始 JPA 全流程测试 ==========");

        // --- 1. 使用 DTO 注册用户 (验证 UserService 重构) ---
        System.out.println(">>> 1. 注册管理员...");
        RegisterDTO adminDto = new RegisterDTO();
        adminDto.setUsername("jpa_admin");
        adminDto.setPassword("123456");
        adminDto.setNickname("JPA管理员");
        adminDto.setEmail("admin@jpa.com");
        userService.register(adminDto);

        // 只有手动查库才能拿到 ID，因为 register 方法返回 void
        User admin = userRepository.findByUsername("jpa_admin");
        // 手动升级权限
        admin.setRole("PUBLISHER");
        userRepository.save(admin);
        Long adminId = admin.getId();

        System.out.println(">>> 2. 注册普通用户...");
        RegisterDTO userDto = new RegisterDTO();
        userDto.setUsername("jpa_user");
        userDto.setPassword("123456");
        userDto.setNickname("JPA学生");
        userDto.setEmail("user@jpa.com");
        userService.register(userDto);
        User student = userRepository.findByUsername("jpa_user");
        Long userId = student.getId();


        // --- 2. 发布任务 (验证 JPA Repository) ---
        System.out.println(">>> 3. 发布任务...");
        UserContext.setUserId(adminId); // 模拟上下文

        Task task = new Task();
        task.setUserId(adminId);
        task.setTitle("JPA重构测试任务");
        task.setDescription("测试DTO和JPA");
        task.setDueDate(LocalDateTime.now().plusDays(7));

        // 注意：如果你还没把 TaskService 改成接收 DTO，这里传 Entity 是对的
        taskService.createTask(task, null);
        Long taskId = task.getId();
        Assertions.assertNotNull(taskId, "任务ID不应为空");


        // --- 3. 提交文件 (验证 SubmissionService + OSS) ---
        System.out.println(">>> 4. 提交文件...");
        UserContext.setUserId(userId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "jpa_test.txt", "text/plain", "Hello JPA".getBytes()
        );
        submissionService.submitFile(taskId, userId, file);


        // --- 4. 验证复杂 JPQL 查询 (UserHistoryVO) ---
        System.out.println(">>> 5. 验证报表查询 (UserHistoryVO)...");

        // 这一步最容易报错，如果 UserHistoryVO 构造函数没写对，这里会挂
        List<UserHistoryVO> history = submissionRepository.findHistoryByUserId(userId);

        Assertions.assertEquals(1, history.size());
        UserHistoryVO record = history.get(0);

        System.out.println("查到的任务标题: " + record.getTaskTitle());
        System.out.println("查到的文件名: " + record.getFileName());

        Assertions.assertEquals("JPA重构测试任务", record.getTaskTitle());
        Assertions.assertEquals("jpa_test.txt", record.getFileName());

        System.out.println("========== JPA 测试全部通过！ ==========");
    }
}
