package io.github.emresurgun.benchmark.target.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PingPongHandler pingPongHandler;

    public WebSocketConfig(PingPongHandler pingPongHandler) {
        this.pingPongHandler = pingPongHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pingPongHandler, "/ws")
                .setAllowedOrigins("*");
    }
}