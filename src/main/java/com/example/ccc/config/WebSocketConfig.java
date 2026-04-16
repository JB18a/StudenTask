package com.example.ccc.config;

import com.example.ccc.websocket.GradeNotificationHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GradeNotificationHandler gradeNotificationHandler;

    public WebSocketConfig(GradeNotificationHandler gradeNotificationHandler) {
        this.gradeNotificationHandler = gradeNotificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gradeNotificationHandler, "/ws/grade")
                .setAllowedOrigins("*");
    }
}
