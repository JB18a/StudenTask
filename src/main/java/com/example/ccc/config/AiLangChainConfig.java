package com.example.ccc.config;

import com.example.ccc.ai.AiAssistant;
import com.example.ccc.ai.DatabaseTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiLangChainConfig {

    @Bean
    public OpenAiStreamingChatModel bailianStreamingChatModel(BailianProperties bailianProperties) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(bailianProperties.getBaseUrl())
                .apiKey(bailianProperties.getApiKey())
                .modelName(bailianProperties.getModel())
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.withMaxMessages(20);
    }

    @Bean
    public AiAssistant aiAssistant(OpenAiStreamingChatModel bailianStreamingChatModel,
                                  ChatMemoryProvider chatMemoryProvider,
                                  DatabaseTools databaseTools) {
        return AiServices.builder(AiAssistant.class)
                .streamingChatModel(bailianStreamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(databaseTools)
                .build();
    }
}

