package com.example.oa.portal.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.*;

/**
 * WebSocket 演示用 handler。
 * 用来简单展示：建立长连接、服务端定时发心跳、以及回显客户端发来的文本。
 */
@Slf4j
public class DemoWebSocketHandler extends TextWebSocketHandler {

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "ws-demo-heartbeat");
                t.setDaemon(true);
                return t;
            });

    private static final Map<String, ScheduledFuture<?>> HEARTBEATS = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String id = session.getId();
        log.info("WebSocket demo connected: {}", id);
        sendSafe(session, "{\"type\":\"connected\",\"message\":\"WebSocket 长连接已建立\"}");

        ScheduledFuture<?> future = SCHEDULER.scheduleAtFixedRate(() -> {
            if (!session.isOpen()) {
                ScheduledFuture<?> f = HEARTBEATS.remove(id);
                if (f != null) {
                    f.cancel(false);
                }
                return;
            }
            String payload = String.format(
                    "{\"type\":\"heartbeat\",\"time\":\"%s\",\"ts\":%d}",
                    FORMATTER.format(Instant.now()),
                    System.currentTimeMillis());
            sendSafe(session, payload);
        }, 5, 5, TimeUnit.SECONDS);

        HEARTBEATS.put(id, future);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("WebSocket demo received from {}: {}", session.getId(), payload);
        sendSafe(session, "{\"type\":\"echo\",\"message\":" + toJsonString(payload) + "}");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String id = session.getId();
        log.info("WebSocket demo disconnected: {}, status={}", id, status);
        ScheduledFuture<?> f = HEARTBEATS.remove(id);
        if (f != null) {
            f.cancel(false);
        }
    }

    private void sendSafe(WebSocketSession session, String json) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.debug("WebSocket demo send failed: {}", e.getMessage());
        }
    }

    private String toJsonString(String text) {
        if (text == null) {
            return "null";
        }
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}

