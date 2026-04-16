package com.example.ccc.service;

import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.utils.PdfTextExtractor;
import com.example.ccc.utils.TencentCosService;
import com.example.ccc.utils.WordTextExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AiGradingService {

    private static final Logger log = LoggerFactory.getLogger(AiGradingService.class);

    @Autowired
    private TencentCosService cosService;

    @Autowired
    private PdfTextExtractor pdfTextExtractor;

    @Autowired
    private WordTextExtractor wordTextExtractor;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${bailian.base-url}")
    private String baseUrl;

    @Value("${bailian.api-key}")
    private String apiKey;

    @Value("${bailian.model}")
    private String model;

    public void gradeSubmission(Long submissionId) {
        TaskSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.error("提交记录不存在: {}", submissionId);
            return;
        }

        Task task = taskRepository.findById(submission.getTaskId()).orElse(null);
        if (task == null) {
            log.error("任务不存在: {}", submission.getTaskId());
            return;
        }

        try {
            String content = downloadAndExtractContent(submission.getFileUrl(), submission.getFileName());

            if (content == null || content.trim().isEmpty()) {
                submission.setAiSummary("无法提取文件内容");
                submission.setAiSuggestion("文件内容为空或无法解析，请检查文件格式是否正确。支持格式：.txt, .pdf, .doc, .docx");
                submission.setAiAnalysisStatus("FAILED");
                submissionRepository.save(submission);
                return;
            }

            if (content.startsWith("[") && content.contains("失败")) {
                submission.setAiSummary("文件解析异常");
                submission.setAiSuggestion(content);
                submission.setAiAnalysisStatus("FAILED");
                submissionRepository.save(submission);
                return;
            }

            String gradingResult = analyzeWithAI(content, task, submission);

            Map<String, Object> resultMap = parseAIResponse(gradingResult);

            String summary = extractSummary(resultMap);
            String suggestion = extractSuggestion(resultMap);

            submission.setAiSummary(summary);
            submission.setAiSuggestion(suggestion);
            submission.setAiAnalysisStatus("COMPLETED");
            submission.setAiAnalysisTime(LocalDateTime.now());
            submissionRepository.save(submission);

            log.info("AI初评完成 submissionId={}", submissionId);

        } catch (Exception e) {
            log.error("AI初评失败 submissionId={}", submissionId, e);
            submission.setAiSummary("AI分析失败");
            submission.setAiSuggestion("分析过程中发生错误: " + e.getMessage());
            submission.setAiAnalysisStatus("FAILED");
            submissionRepository.save(submission);
        }
    }

    private String downloadAndExtractContent(String fileUrl, String fileName) {
        try {
            byte[] fileBytes = cosService.downloadFile(fileUrl);

            if (fileBytes == null || fileBytes.length == 0) {
                return "[文件下载失败或文件为空]";
            }

            String lowerName = fileName != null ? fileName.toLowerCase() : "";

            if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
                log.info("检测到Word文档: {}", fileName);
                return wordTextExtractor.extractText(fileBytes, fileName);
            } else if (lowerName.endsWith(".pdf")) {
                log.info("检测到PDF文档: {}", fileName);
                return pdfTextExtractor.extractTextFromPdf(fileBytes);
            } else if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
                log.info("检测到文本文件: {}", fileName);
                return pdfTextExtractor.extractTextFromTextFile(fileBytes);
            } else {
                log.info("未知文件类型，尝试作为文本解析: {}", fileName);
                String text = new String(fileBytes, StandardCharsets.UTF_8);
                if (text.contains("\ufffd") || text.length() < fileBytes.length / 4) {
                    return "[不支持的文件格式: " + fileName + "，请上传 .txt, .pdf, .doc 或 .docx 格式的文件]";
                }
                return text;
            }
        } catch (Exception e) {
            log.error("文件内容提取失败: {}", fileName, e);
            return "[文件内容提取失败: " + e.getMessage() + "]";
        }
    }

    private String analyzeWithAI(String content, Task task, TaskSubmission submission) {
        String truncatedContent = content.length() > 8000 ? content.substring(0, 8000) : content;

        String taskTitle = task.getTitle() != null ? task.getTitle() : "未命名作业";
        String taskDesc = task.getDescription() != null ? task.getDescription() : "无具体要求";

        String prompt = String.format("""
            你是一位专业的作业批改AI助手。请仔细分析学生的作业内容，并给出专业的评价和建议。

            ## 作业信息
            - 作业标题：%s
            - 作业要求：%s
            - 学生姓名：%s
            - 提交文件：%s

            ## 学生作业内容
            ```
            %s
            ```

            ## 分析要求
            请按以下步骤进行分析：

            1. **内容识别**：首先识别学生作业中的具体内容，包括：
               - 数学题：识别题目和学生的答案
               - 文字题：识别学生的回答内容
               - 如果是数学计算题，请验证计算是否正确

            2. **正确性判断**：
               - 对于数学题，逐一判断每道题的答案是否正确
               - 如果答案错误，请指出正确答案
               - 对于主观题，评估回答的完整性和准确性

            3. **评分建议**：
               - 根据正确率给出建议分数（0-100分）
               - 列出具体的得分点和扣分点

            ## 返回格式
            请严格按照以下JSON格式返回（不要包含其他文字）：
            {
                "summary": "作业内容概述（50字以内，说明学生做了什么）",
                "correctAnswers": ["第1题答案正确", "第2题答案正确"],
                "wrongAnswers": ["第3题答案错误，正确答案应为X"],
                "strengths": ["优点1", "优点2"],
                "weaknesses": ["不足1", "不足2"],
                "suggestedScore": 85,
                "suggestion": "详细的批改建议，包括每道题的对错分析和改进建议"
            }

            注意：
            - 必须仔细分析作业内容，不能简单地说"无法判断"
            - 对于数学计算题，必须验证计算结果
            - suggestion字段要具体，包含每道题的批改意见
            """,
            taskTitle,
            taskDesc,
            "学生",
            submission.getFileName() != null ? submission.getFileName() : "未知",
            truncatedContent
        );

        return callBailianAPI(prompt);
    }

    private String callBailianAPI(String prompt) {
        try {
            java.net.URL url = new java.net.URL(baseUrl + "/chat/completions");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3
            );

            String jsonInput = objectMapper.writeValueAsString(requestBody);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    JsonNode rootNode = objectMapper.readTree(response.toString());
                    JsonNode choicesNode = rootNode.path("choices");
                    if (choicesNode.isArray() && choicesNode.size() > 0) {
                        JsonNode messageNode = choicesNode.get(0).path("message");
                        return messageNode.path("content").asText();
                    }
                }
            }

            log.error("Bailian API调用失败: responseCode={}", responseCode);
            return createDefaultResponse();

        } catch (Exception e) {
            log.error("Bailian API调用异常", e);
            return createDefaultResponse();
        }
    }

    private String createDefaultResponse() {
        return "{\"summary\": \"AI服务暂时不可用\", \"correctAnswers\": [], \"wrongAnswers\": [], \"strengths\": [], \"weaknesses\": [], \"suggestedScore\": 0, \"suggestion\": \"AI分析服务暂时不可用，请稍后重试或手动批改\"}";
    }

    private Map<String, Object> parseAIResponse(String response) {
        try {
            String jsonStr = response;
            if (response.contains("```json")) {
                jsonStr = response.substring(response.indexOf("```json") + 7);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            } else if (response.contains("```")) {
                jsonStr = response.substring(response.indexOf("```") + 3);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            }

            jsonStr = jsonStr.trim();
            if (!jsonStr.startsWith("{")) {
                int start = jsonStr.indexOf("{");
                if (start >= 0) {
                    jsonStr = jsonStr.substring(start);
                }
            }

            return objectMapper.readValue(jsonStr, Map.class);
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", response, e);
            return Map.of(
                "summary", "解析失败",
                "suggestedScore", 0,
                "suggestion", response
            );
        }
    }

    private String extractSummary(Map<String, Object> resultMap) {
        Object summary = resultMap.get("summary");
        return summary != null ? summary.toString() : "无总结";
    }

    private String extractSuggestion(Map<String, Object> resultMap) {
        StringBuilder sb = new StringBuilder();

        Object correctAnswers = resultMap.get("correctAnswers");
        if (correctAnswers instanceof List && !((List<?>) correctAnswers).isEmpty()) {
            sb.append("✅ 正确答案：\n");
            for (Object ans : (List<?>) correctAnswers) {
                sb.append("  • ").append(ans).append("\n");
            }
        }

        Object wrongAnswers = resultMap.get("wrongAnswers");
        if (wrongAnswers instanceof List && !((List<?>) wrongAnswers).isEmpty()) {
            sb.append("❌ 错误答案：\n");
            for (Object ans : (List<?>) wrongAnswers) {
                sb.append("  • ").append(ans).append("\n");
            }
        }

        Object strengths = resultMap.get("strengths");
        if (strengths instanceof List && !((List<?>) strengths).isEmpty()) {
            sb.append("💪 优点：\n");
            for (Object s : (List<?>) strengths) {
                sb.append("  • ").append(s).append("\n");
            }
        }

        Object weaknesses = resultMap.get("weaknesses");
        if (weaknesses instanceof List && !((List<?>) weaknesses).isEmpty()) {
            sb.append("📝 不足：\n");
            for (Object w : (List<?>) weaknesses) {
                sb.append("  • ").append(w).append("\n");
            }
        }

        Object suggestedScore = resultMap.get("suggestedScore");
        if (suggestedScore != null) {
            sb.append("📊 建议分数：").append(suggestedScore).append("分\n");
        }

        Object suggestion = resultMap.get("suggestion");
        if (suggestion != null) {
            sb.append("💡 批改建议：").append(suggestion);
        }

        String result = sb.toString();
        return result.isEmpty() ? "暂无详细建议" : result;
    }
}