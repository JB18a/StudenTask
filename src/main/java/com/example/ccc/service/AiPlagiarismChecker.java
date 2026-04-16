package com.example.ccc.service;

import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.AiAnalysisMessage;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.utils.PdfTextExtractor;
import com.example.ccc.utils.TencentCosService;
import com.example.ccc.utils.WordTextExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AiPlagiarismChecker {

    private static final Logger log = LoggerFactory.getLogger(AiPlagiarismChecker.class);

    @Autowired
    private TencentCosService cosService;

    @Autowired
    private PdfTextExtractor pdfTextExtractor;

    @Autowired
    private WordTextExtractor wordTextExtractor;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${bailian.base-url}")
    private String baseUrl;

    @Value("${bailian.api-key}")
    private String apiKey;

    @Value("${bailian.model}")
    private String model;

    public void checkPlagiarism(Long submissionId) {
        TaskSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.error("提交记录不存在: {}", submissionId);
            return;
        }

        submission.setAiAnalysisStatus("CHECKING");
        submissionRepository.save(submission);

        try {
            String content = downloadAndExtractContent(submission.getFileUrl(), submission.getFileName());

            if (content == null || content.trim().isEmpty()) {
                submission.setAiAnalysisStatus("FAILED");
                submission.setAiPlagiarismDetails("无法提取文件内容");
                submissionRepository.save(submission);
                return;
            }

            if (content.startsWith("[") && content.contains("失败")) {
                submission.setAiAnalysisStatus("FAILED");
                submission.setAiPlagiarismDetails(content);
                submissionRepository.save(submission);
                return;
            }

            String plagiarismReport = analyzePlagiarismWithAI(content, submissionId);

            Map<String, Object> reportMap = parseAIResponse(plagiarismReport);

            Double similarityScore = extractSimilarityScore(reportMap);
            String details = extractDetails(reportMap);

            submission.setAiPlagiarismScore(similarityScore);
            submission.setAiPlagiarismDetails(details);
            submission.setAiAnalysisStatus("CHECKING");
            submission.setAiAnalysisTime(LocalDateTime.now());
            submissionRepository.save(submission);

            log.info("查重完成并保存 submissionId={}, score={}", submissionId, similarityScore);

        } catch (Exception e) {
            log.error("查重失败 submissionId={}", submissionId, e);
            submission.setAiAnalysisStatus("FAILED");
            submission.setAiPlagiarismDetails("查重失败: " + e.getMessage());
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
                    return "[不支持的文件格式: " + fileName + "]";
                }
                return text;
            }
        } catch (Exception e) {
            log.error("文件内容提取失败: {}", fileName, e);
            return "[文件内容提取失败: " + e.getMessage() + "]";
        }
    }

    private String analyzePlagiarismWithAI(String content, Long submissionId) {
        String truncatedContent = content.length() > 8000 ? content.substring(0, 8000) : content;

        String prompt = String.format("""
            你是一个专业的学术查重助手。请分析以下学生作业文本的原创性。

            待分析文本：
            ```
            %s
            ```

            请以JSON格式返回分析结果，包含以下字段：
            {
                "similarity_score": 相似度百分比(0-100的数字),
                "similar_parts": ["相似片段1", "相似片段2"],
                "possible_sources": ["可能的来源1", "可能的来源2"],
                "summary": "总体分析总结"
            }

            分析要点：
            1. 如果是数学计算题答案，相似度通常较低，因为计算结果是唯一的
            2. 如果是标准答案或公式，相似度可能较高，但这是正常的
            3. 如果是完整抄袭的段落，相似度会很高
            4. 请根据内容的独特性给出合理的相似度评分

            注意：只返回JSON，不要包含其他文字。
            """, truncatedContent);

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
        return "{\"similarity_score\": 0, \"similar_parts\": [], \"possible_sources\": [], \"summary\": \"分析服务暂时不可用\"}";
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
            return Map.of("similarity_score", 0, "summary", response);
        }
    }

    private Double extractSimilarityScore(Map<String, Object> reportMap) {
        Object score = reportMap.get("similarity_score");
        if (score instanceof Number) {
            return ((Number) score).doubleValue();
        }
        return 0.0;
    }

    private String extractDetails(Map<String, Object> reportMap) {
        StringBuilder sb = new StringBuilder();

        Object similarParts = reportMap.get("similar_parts");
        if (similarParts instanceof List && !((List<?>) similarParts).isEmpty()) {
            sb.append("相似片段：\n");
            for (Object part : (List<?>) similarParts) {
                sb.append("  • ").append(part).append("\n");
            }
        }

        Object possibleSources = reportMap.get("possible_sources");
        if (possibleSources instanceof List && !((List<?>) possibleSources).isEmpty()) {
            sb.append("可能来源：\n");
            for (Object source : (List<?>) possibleSources) {
                sb.append("  • ").append(source).append("\n");
            }
        }

        Object summary = reportMap.get("summary");
        if (summary != null) {
            sb.append("总结：").append(summary);
        }

        String result = sb.toString();
        return result.isEmpty() ? reportMap.toString() : result;
    }

    public void triggerAnalysis(Long submissionId) {
        TaskSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            throw new RuntimeException("提交记录不存在");
        }

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.AI_ANALYSIS_EXCHANGE,
            RabbitMQConfig.AI_ANALYSIS_ROUTING_KEY,
            new AiAnalysisMessage(
                submission.getId(),
                submission.getTaskId(),
                submission.getUserId(),
                submission.getFileUrl(),
                submission.getFileName()
            )
        );

        submission.setAiAnalysisStatus("QUEUED");
        submissionRepository.save(submission);
    }
}