package com.example.ccc.service;

import com.example.ccc.config.TestMockConfig;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.utils.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMockConfig.class)
@Transactional
class AiGradingIntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AiAsyncService aiAsyncService;

    @Autowired
    private AiPlagiarismChecker plagiarismChecker;

    @Autowired
    private AiGradingService gradingService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    private Long testUserId;
    private Long testTaskId;

    @BeforeEach
    void setUp() {
        List<User> users = userRepository.findByRole("USER");
        if (!users.isEmpty()) {
            testUserId = users.get(0).getId();
            System.out.println("使用测试用户ID: " + testUserId);
        } else {
            fail("没有找到USER角色的用户，请先创建用户");
        }

        List<Task> tasks = taskRepository.findAll();
        if (!tasks.isEmpty()) {
            testTaskId = tasks.get(0).getId();
            System.out.println("使用测试任务ID: " + testTaskId);
        } else {
            Task task = new Task();
            task.setTitle("数学作业测试");
            task.setDescription("简单的加减法练习");
            task.setUserId(testUserId);
            task = taskRepository.save(task);
            testTaskId = task.getId();
            System.out.println("创建新测试任务ID: " + testTaskId);
        }

        UserContext.setUserId(testUserId);
    }

    @Test
    void testAiGradingFlow() throws Exception {
        String content = "学生姓名: 迷人的牛爷爷\n" +
                "学号: 2021001234\n\n" +
                "一、加法计算 (每题10分)\n" +
                "1. 25 + 17 = 42\n" +
                "2. 156 + 289 = 445\n" +
                "3. 1234 + 5678 = 6912\n\n" +
                "二、减法计算 (每题10分)\n" +
                "1. 100 - 37 = 63\n" +
                "2. 500 - 189 = 311\n" +
                "3. 1000 - 456 = 544\n\n" +
                "三、应用题 (每题25分)\n" +
                "1. 小明有25个苹果，给了小红8个，还剩多少个？\n" +
                "   答：25 - 8 = 17（个）\n\n" +
                "2. 商店进货120箱牛奶，卖出45箱，又进货30箱，现在有多少箱？\n" +
                "   答：120 - 45 + 30 = 105（箱）";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "math_homework.txt",
                "text/plain",
                content.getBytes());

        System.out.println("===== 开始提交作业 =====");
        System.out.println("任务ID: " + testTaskId);
        System.out.println("用户ID: " + testUserId);
        System.out.println("文件内容预览: " + content.substring(0, Math.min(100, content.length())));

        submissionService.submitFile(testTaskId, testUserId, file);

        List<TaskSubmission> submissions = submissionRepository.findByTaskId(testTaskId);
        assertFalse(submissions.isEmpty(), "应该有提交记录");

        TaskSubmission submission = submissions.get(submissions.size() - 1);
        Long submissionId = submission.getId();

        System.out.println("提交记录ID: " + submissionId);
        System.out.println("文件URL: " + submission.getFileUrl());
        System.out.println("AI分析状态: " + submission.getAiAnalysisStatus());

        assertNotNull(submission.getFileUrl(), "文件URL不应为空");
        assertEquals("QUEUED", submission.getAiAnalysisStatus(), "状态应为QUEUED");

        System.out.println("\n===== 手动调用AI服务进行测试 =====");

        plagiarismChecker.checkPlagiarism(submissionId);
        gradingService.gradeSubmission(submissionId);

        submission = submissionRepository.findById(submissionId).orElse(null);
        assertNotNull(submission, "提交记录不应为空");

        System.out.println("\n===== AI分析结果 =====");
        System.out.println("状态: " + submission.getAiAnalysisStatus());
        System.out.println("总结: " + submission.getAiSummary());
        System.out.println("建议: " + submission.getAiSuggestion());
        System.out.println("查重分数: " + submission.getAiPlagiarismScore());
        System.out.println("分析时间: " + submission.getAiAnalysisTime());

        if ("COMPLETED".equals(submission.getAiAnalysisStatus())) {
            assertNotNull(submission.getAiSummary(), "AI总结不应为空");
            assertNotNull(submission.getAiSuggestion(), "AI建议不应为空");
            System.out.println("\n✅ AI分析成功完成！");
        } else if ("FAILED".equals(submission.getAiAnalysisStatus())) {
            System.out.println("\n⚠️ AI分析失败，详情: " + submission.getAiPlagiarismDetails());
        }
    }

    @Test
    void testManualTriggerAiAnalysis() throws Exception {
        String content = "学生姓名: 测试学生\n" +
                "学号: 12345\n\n" +
                "一、加法计算\n" +
                "1. 10 + 20 = 30\n" +
                "2. 5 + 15 = 20\n\n" +
                "二、减法计算\n" +
                "1. 50 - 10 = 40\n" +
                "2. 100 - 25 = 75";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "simple_math.txt",
                "text/plain",
                content.getBytes());

        System.out.println("===== 测试手动触发AI分析 =====");
        submissionService.submitFile(testTaskId, testUserId, file);

        List<TaskSubmission> submissions = submissionRepository.findByTaskId(testTaskId);
        TaskSubmission submission = submissions.get(submissions.size() - 1);
        Long submissionId = submission.getId();

        System.out.println("提交ID: " + submissionId);
        System.out.println("当前状态: " + submission.getAiAnalysisStatus());

        aiAsyncService.triggerAiAnalysisAsync(submissionId);

        submission = submissionRepository.findById(submissionId).orElse(null);
        assertNotNull(submission);
        assertEquals("QUEUED", submission.getAiAnalysisStatus());

        plagiarismChecker.checkPlagiarism(submissionId);
        gradingService.gradeSubmission(submissionId);

        submission = submissionRepository.findById(submissionId).orElse(null);
        assertNotNull(submission);

        System.out.println("\n===== 手动触发测试结果 =====");
        System.out.println("状态: " + submission.getAiAnalysisStatus());
        System.out.println("总结: " + submission.getAiSummary());
        System.out.println("建议: " + submission.getAiSuggestion());
        System.out.println("查重分数: " + submission.getAiPlagiarismScore());

        assertEquals("COMPLETED", submission.getAiAnalysisStatus(), "状态应为COMPLETED");
        assertNotNull(submission.getAiSummary(), "AI总结不应为空");
        assertNotNull(submission.getAiSuggestion(), "AI建议不应为空");
    }

    @Test
    void testAiAnalysisStatusQuery() throws Exception {
        String content = "2 + 3 = 5\n10 - 4 = 6";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content.getBytes());

        System.out.println("===== 测试AI分析状态查询 =====");
        submissionService.submitFile(testTaskId, testUserId, file);

        List<TaskSubmission> submissions = submissionRepository.findByTaskId(testTaskId);
        TaskSubmission submission = submissions.get(submissions.size() - 1);
        Long submissionId = submission.getId();

        System.out.println("提交ID: " + submissionId);

        plagiarismChecker.checkPlagiarism(submissionId);
        gradingService.gradeSubmission(submissionId);

        submission = submissionRepository.findById(submissionId).orElse(null);
        assertNotNull(submission);

        System.out.println("\n===== 状态查询结果 =====");
        System.out.println("AI分析状态: " + submission.getAiAnalysisStatus());
        System.out.println("AI总结: " + submission.getAiSummary());
        System.out.println("AI建议: " + submission.getAiSuggestion());
        System.out.println("查重分数: " + submission.getAiPlagiarismScore());
        System.out.println("分析时间: " + submission.getAiAnalysisTime());

        assertNotNull(submission.getAiAnalysisStatus());
    }
}