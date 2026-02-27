package com.example.oa.portal.config;

import com.example.oa.portal.websocket.DemoWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(demoWebSocketHandler(), "/ws/demo")
                .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler demoWebSocketHandler() {
        return new DemoWebSocketHandler();
    }
}

