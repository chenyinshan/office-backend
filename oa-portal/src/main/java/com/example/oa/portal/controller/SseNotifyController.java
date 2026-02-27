package com.example.oa.portal.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个最简单的 SSE（Server-Sent Events）长连接接口。
 * 对比短连接 REST：这里连上之后连接会保持一段时间，服务端隔一会儿就往前端推一条心跳。
 * 可以用来演示「长连接 + 服务端推送」的基本交互。
 */
@RestController
@RequestMapping("/api/sse")
@Slf4j
public class SseNotifyController {

    private static final long HEARTBEAT_INTERVAL_SEC = 15;
    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L; // 5 分钟

    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    /**
     * SSE 长连接端点：建立后服务端每 15 秒推送一次心跳，用于演示长连接与服务端推送。
     * 前端建议带 Accept: text/event-stream 以便匹配；fetch 时显式设置 Accept 头。
     */
    @GetMapping(value = "/notify", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseNotify() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        AtomicLong seq = new AtomicLong(0);

        emitter.onCompletion(() -> log.debug("SSE 客户端完成，连接关闭"));
        emitter.onTimeout(() -> log.debug("SSE 超时，连接关闭"));
        emitter.onError(e -> log.warn("SSE 异常: {}", e.getMessage()));

        scheduler.scheduleAtFixedRate(() -> {
            try {
                long n = seq.incrementAndGet();
                String payload = String.format("{\"type\":\"heartbeat\",\"seq\":%d,\"ts\":%d}",
                        n, System.currentTimeMillis());
                emitter.send(SseEmitter.event().data(payload));
            } catch (IOException e) {
                log.debug("SSE 发送失败，连接可能已关闭: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);

        // 立即发一条欢迎事件，便于前端确认连接成功
        try {
            emitter.send(SseEmitter.event().data("{\"type\":\"connected\",\"message\":\"SSE 长连接已建立\"}"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
