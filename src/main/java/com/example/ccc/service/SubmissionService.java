package com.example.ccc.service;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskRepository; // 改用 Repo
import com.example.ccc.repository.TaskSubmissionRepository; // 改用 Repo
import com.example.ccc.repository.UserRepository;
import com.example.ccc.utils.TencentCosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class SubmissionService {

    // 【修复代码】注入 Repository，而不是 Mapper
    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TencentCosService cosService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AiAsyncService aiAsyncService;

    @Transactional(rollbackFor = Exception.class)
    public void submitFile(Long taskId, Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 检查任务是否存在 (JPA 写法)
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        // 上传 COS
        String url = cosService.uploadFile(file);

        // 存库
        TaskSubmission submission = new TaskSubmission();
        submission.setTaskId(taskId);
        submission.setUserId(userId);
        submission.setFileUrl(url);
        submission.setFileName(file.getOriginalFilename());
        submission.setAiAnalysisStatus("PENDING");

        TaskSubmission saved = submissionRepository.save(submission);

        aiAsyncService.triggerAiAnalysisAsync(saved.getId());
    }

    public List<TaskSubmission> getSubmissionsForTask(Long taskId) {
        // JPA 写法
        return submissionRepository.findByTaskId(taskId);
    }

    /**
     * 【新增】获取未提交人员名单
     */
    public List<User> getUnsubmittedUsers(Long taskId) {
        return userRepository.findUnsubmittedUsers(taskId);
    }
}