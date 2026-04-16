package com.example.ccc.consumer;

import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.AiAnalysisMessage;
import com.example.ccc.service.AiGradingService;
import com.example.ccc.service.AiPlagiarismChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiAnalysisConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisConsumer.class);

    @Autowired
    private AiPlagiarismChecker plagiarismChecker;

    @Autowired
    private AiGradingService gradingService;

    @RabbitListener(queues = RabbitMQConfig.AI_ANALYSIS_QUEUE)
    public void handleAiAnalysis(AiAnalysisMessage message) {
        log.info("收到AI分析任务: submissionId={}, taskId={}, fileName={}",
                message.getSubmissionId(), message.getTaskId(), message.getFileName());

        try {
            plagiarismChecker.checkPlagiarism(message.getSubmissionId());

            gradingService.gradeSubmission(message.getSubmissionId());

            log.info("AI分析任务完成: submissionId={}", message.getSubmissionId());

        } catch (Exception e) {
            log.error("AI分析任务失败: submissionId={}", message.getSubmissionId(), e);
        }
    }
}