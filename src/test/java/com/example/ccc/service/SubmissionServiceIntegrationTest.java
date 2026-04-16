package com.example.ccc.service;

import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.utils.TencentCosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class SubmissionServiceIntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private TencentCosService tencentCosService;

    private Long testTaskId;
    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        Task task = new Task();
        task.setTitle("测试任务");
        task.setDescription("集成测试任务");
        task = taskRepository.save(task);
        testTaskId = task.getId();
    }

    @Test
    void submitFile_shouldUploadToCosAndSaveSubmission() throws Exception {
        String filePath = "C:\\Users\\迷人的牛爷爷\\Pictures\\Screenshots\\屏幕截图 2025-11-25 083744.png";
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            System.out.println("测试文件不存在，跳过实际文件测试");
            assertTrue(Files.exists(path), "测试文件不存在: " + filePath);
            return;
        }

        byte[] fileContent = Files.readAllBytes(path);
        String originalFilename = path.getFileName().toString();

        MockMultipartFile file = new MockMultipartFile(
            "file",
            originalFilename,
            "image/png",
            fileContent
        );

        System.out.println("文件大小: " + fileContent.length + " bytes");
        System.out.println("文件名: " + originalFilename);

        submissionService.submitFile(testTaskId, testUserId, file);

        var submissions = submissionRepository.findByTaskId(testTaskId);
        assertFalse(submissions.isEmpty(), "应该至少有一条提交记录");

        TaskSubmission submission = submissions.get(submissions.size() - 1);
        System.out.println("提交后的文件URL: " + submission.getFileUrl());

        assertNotNull(submission.getFileUrl(), "文件URL不应为空");
        assertTrue(submission.getFileUrl().contains("czzz-1412464270.cos.ap-nanjing.myqcloud.com"),
            "URL应包含COS域名");
        assertTrue(submission.getFileUrl().endsWith(".png"), "URL应以.png结尾");
        assertEquals(originalFilename, submission.getFileName(), "文件名应保持一致");
    }

    @Test
    void submitFile_shouldWorkWithSmallFile() {
        MockMultipartFile smallFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Hello COS".getBytes()
        );

        submissionService.submitFile(testTaskId, testUserId, smallFile);

        var submissions = submissionRepository.findByTaskId(testTaskId);
        assertFalse(submissions.isEmpty());

        TaskSubmission submission = submissions.get(submissions.size() - 1);
        System.out.println("小文件URL: " + submission.getFileUrl());

        assertNotNull(submission.getFileUrl());
        assertTrue(submission.getFileUrl().contains("czzz-1412464270.cos.ap-nanjing.myqcloud.com"));
    }
}