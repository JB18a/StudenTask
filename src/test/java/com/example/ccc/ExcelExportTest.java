package com.example.ccc;
import com.example.ccc.controller.AdminUserController;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.service.SubmissionService;
import com.example.ccc.service.TaskService;
import com.example.ccc.utils.UserContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class ExcelExportTest {

    @Autowired
    private AdminUserController adminUserController;

    @Autowired
    private UserRepository userRepository; // ✅ Repository

    @Autowired
    private TaskService taskService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private TaskSubmissionRepository submissionRepository; // ✅ Repository

    private Long getOrCreateUser(String username, String nickname, String role) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setPassword("123456");
            user.setNickname(nickname);
            user.setRole(role);
            userRepository.save(user);
        } else {
            user.setRole(role);
            userRepository.save(user);
        }
        return user.getId();
    }

    @Test
    void testExportExcelFile() throws Exception {
        System.out.println("========== 开始 Excel 导出测试 (JPA版) ==========");

        // --- 1. 准备数据 ---
        Long adminId = getOrCreateUser("admin_excel_jpa", "JPA导出管理员", "PUBLISHER");
        Long userId = getOrCreateUser("user_excel_jpa", "JPA导出学生", "USER");

        // 发任务
        UserContext.setUserId(adminId);
        Task task = new Task();
        task.setUserId(adminId);
        task.setTitle("JPA Excel导出测试");
        task.setDescription("测试数据");
        task.setDueDate(LocalDateTime.now().plusDays(5));
        taskService.createTask(task, null);
        Long taskId = task.getId();

        // 交作业
        UserContext.setUserId(userId);
        MockMultipartFile file = new MockMultipartFile("file", "test_jpa.docx", "text/plain", "abc".getBytes());
        submissionService.submitFile(taskId, userId, file);

        // 打分 (JPA 方式)
        UserContext.setUserId(adminId);
        List<TaskSubmission> list = submissionRepository.findByTaskId(taskId);
        TaskSubmission sub = list.get(0);
        sub.setScore(88);
        sub.setFeedback("Excel测试评语JPA");
        submissionRepository.save(sub); // 保存更新

        System.out.println(">>> 数据准备完成，开始测试导出接口...");


        // --- 2. 模拟调用导出接口 ---
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserContext.setUserId(adminId); // 切换管理员权限

        adminUserController.exportUserReport(userId, response);


        // --- 3. 验证结果 ---
        if (response.getStatus() != 200) {
            throw new RuntimeException("导出失败，状态码: " + response.getStatus());
        }

        System.out.println("Content-Type: " + response.getContentType());
        byte[] fileBytes = response.getContentAsByteArray();
        System.out.println("生成的 Excel 文件大小: " + fileBytes.length + " bytes");
        Assertions.assertTrue(fileBytes.length > 0, "导出文件不应为空");


        // --- 4. 保存文件到本地查看 ---
        String localFilePath = "test_report_jpa.xlsx";
        File localFile = new File(localFilePath);
        try (FileOutputStream fos = new FileOutputStream(localFile)) {
            fos.write(fileBytes);
        }

        System.out.println("✅ 测试成功！Excel 文件已保存: " + localFile.getAbsolutePath());
    }
}