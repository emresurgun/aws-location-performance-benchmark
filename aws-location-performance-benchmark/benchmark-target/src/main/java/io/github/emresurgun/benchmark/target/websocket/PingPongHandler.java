package io.github.emresurgun.benchmark.target.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class PingPongHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String incomingMessage = message.getPayload();

        if("ping".equals(incomingMessage))
        {
            session.sendMessage(new TextMessage("pong"));
        }
    }
}
