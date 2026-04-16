package com.example.ccc.websocket;

import com.alibaba.fastjson2.JSON;
import com.example.ccc.entity.GradeNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GradeNotificationHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GradeNotificationHandler.class);
    
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.put(userId, session);
            logger.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
            logger.info("WebSocket 连接关闭: userId={}, sessionId={}", userId, session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("收到 WebSocket 消息: {}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    public void sendNotificationToUser(Long userId, GradeNotification notification) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = JSON.toJSONString(notification);
                session.sendMessage(new TextMessage(jsonMessage));
                logger.info("发送成绩通知: userId={}, score={}", userId, notification.getScore());
            } catch (IOException e) {
                logger.error("发送 WebSocket 消息失败: userId={}", userId, e);
                userSessions.remove(userId);
            }
        } else {
            logger.debug("用户不在线，无法发送通知: userId={}", userId);
        }
    }

    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    private Long extractUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            try {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                return Long.parseLong(userIdStr);
            } catch (Exception e) {
                logger.warn("解析 userId 失败: query={}", query);
            }
        }
        return null;
    }
}
