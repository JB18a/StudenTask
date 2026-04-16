package com.example.ccc.service;

import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.AiAnalysisMessage;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.repository.TaskSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiAsyncService {

    private static final Logger log = LoggerFactory.getLogger(AiAsyncService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Async("aiTaskExecutor")
    public void triggerAiAnalysisAsync(Long submissionId) {
        log.info("异步触发AI分析任务: submissionId={}", submissionId);

        try {
            TaskSubmission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission == null) {
                log.error("提交记录不存在: {}", submissionId);
                return;
            }

            submission.setAiAnalysisStatus("QUEUED");
            submissionRepository.save(submission);

            AiAnalysisMessage message = new AiAnalysisMessage(
                submission.getId(),
                submission.getTaskId(),
                submission.getUserId(),
                submission.getFileUrl(),
                submission.getFileName()
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.AI_ANALYSIS_EXCHANGE,
                RabbitMQConfig.AI_ANALYSIS_ROUTING_KEY,
                message
            );

            log.info("AI分析消息已发送到队列: submissionId={}", submissionId);

        } catch (Exception e) {
            log.error("异步触发AI分析失败: submissionId={}", submissionId, e);
            TaskSubmission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission != null) {
                submission.setAiAnalysisStatus("FAILED");
                submission.setAiSuggestion("触发AI分析失败: " + e.getMessage());
                submissionRepository.save(submission);
            }
        }
    }

    @Async("aiAnalysisExecutor")
    public void processAiGradingAsync(Long submissionId) {
        log.info("异步执行AI批改: submissionId={}", submissionId);
    }

    @Async("aiAnalysisExecutor")
    public void processAiPlagiarismCheckAsync(Long submissionId) {
        log.info("异步执行AI查重: submissionId={}", submissionId);
    }
}