package com.example.ccc.controller;

import com.example.ccc.ai.AiAssistant;
import com.example.ccc.common.Result;
import com.example.ccc.dto.AiChatRequest;
import com.example.ccc.utils.UserContext;
import dev.langchain4j.service.TokenStream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AiAssistant aiAssistant;

    public AIController(AiAssistant aiAssistant) {
        this.aiAssistant = aiAssistant;
    }

    /**
     * 非流式（便于调试/兼容）
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatRequest req) {
        Long userId = UserContext.getUserId();
        if (req == null || req.getMessage() == null || req.getMessage().isBlank()) {
            return Result.error(400, "message不能为空");
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        TokenStream stream = aiAssistant.chat(userId, req.getMessage());
        StringBuilder sb = new StringBuilder();
        stream.onPartialResponse(sb::append)
                .onCompleteResponse(resp -> future.complete(sb.toString()))
                .onError(future::completeExceptionally)
                .start();
        try {
            return Result.success(future.join());
        } catch (Exception e) {
            return Result.error(500, "AI调用失败: " + e.getMessage());
        }
    }

    /**
     * SSE 流式输出
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AiChatRequest req) {
        Long userId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(0L);

        if (req == null || req.getMessage() == null || req.getMessage().isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("message不能为空"));
            } catch (IOException ignored) {
            }
            emitter.complete();
            return emitter;
        }

        startStream(emitter, userId, req.getMessage());

        return emitter;
    }

    /**
     * 兼容 EventSource：GET + query 参数
     * 例：/api/ai/chat/stream?message=xxx&token=JWT
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamGet(@RequestParam("message") String message) {
        Long userId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(0L);
        if (message == null || message.isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("message不能为空"));
            } catch (IOException ignored) {
            }
            emitter.complete();
            return emitter;
        }
        startStream(emitter, userId, message);
        return emitter;
    }

    private void startStream(SseEmitter emitter, Long userId, String message) {
        TokenStream stream = aiAssistant.chat(userId, message);
        stream.onPartialResponse(partial -> {
                    try {
                        emitter.send(SseEmitter.event().name("message").data(partial));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .onCompleteResponse(resp -> {
                    try {
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    } catch (IOException ignored) {
                    } finally {
                        emitter.complete();
                    }
                })
                .onError(err -> {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(String.valueOf(err.getMessage())));
                    } catch (IOException ignored) {
                    } finally {
                        emitter.completeWithError(err);
                    }
                })
                .start();
    }
}

