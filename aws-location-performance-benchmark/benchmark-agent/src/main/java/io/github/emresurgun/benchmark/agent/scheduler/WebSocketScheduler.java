package io.github.emresurgun.benchmark.agent.scheduler;

import io.github.emresurgun.benchmark.agent.client.CentralApiClient;
import io.github.emresurgun.benchmark.agent.config.AgentProperties;
import io.github.emresurgun.benchmark.agent.model.MeasurementResult;
import io.github.emresurgun.benchmark.agent.model.MetricType;
import io.github.emresurgun.benchmark.agent.model.TargetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class WebSocketScheduler {

    private final AgentProperties agentProperties;
    private final CentralApiClient centralApiClient;

    private static final Logger log = LoggerFactory.getLogger(WebSocketScheduler.class);

    public WebSocketScheduler(AgentProperties agentProperties, CentralApiClient centralApiClient) {
        this.agentProperties = agentProperties;
        this.centralApiClient = centralApiClient;
    }

    @Scheduled(fixedDelay = 30000)
    public void measureWebSocketRtt() {
        for (TargetConfig target : agentProperties.getTargets()) {
            try {
                String url = "ws://" + target.getHost() + ":" + target.getPort() + "/ws";

                log.info("Measuring WebSocket RTT for target={} url={}",
                        target.getRegion(),
                        url);

                CompletableFuture<Double> rttFuture = new CompletableFuture<>();

                StandardWebSocketClient client = new StandardWebSocketClient();

                TextWebSocketHandler handler = new TextWebSocketHandler() {

                    private long startTime;

                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        startTime = System.nanoTime();
                        session.sendMessage(new TextMessage("ping"));
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                        String payload = message.getPayload();

                        if ("pong".equals(payload)) {
                            long elapsedNanos = System.nanoTime() - startTime;
                            double valueMs = elapsedNanos / 1_000_000.0;

                            rttFuture.complete(valueMs);
                            session.close();
                        }
                    }

                    @Override
                    public void handleTransportError(WebSocketSession session, Throwable exception) {
                        rttFuture.completeExceptionally(exception);
                    }
                };

                client.execute(handler, new WebSocketHttpHeaders(), URI.create(url));

                double valueMs = rttFuture.get(5, TimeUnit.SECONDS);

                log.info("WebSocket RTT target={} valueMs={}",
                        target.getRegion(),
                        valueMs);

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.WEBSOCKET_RTT);
                result.setValueMs(valueMs);
                result.setSuccess(true);
                result.setTimestamp(Instant.now());
                result.setExtraTag(null);

                centralApiClient.send(result);
            }
            catch (Exception e) {
                log.warn("WebSocket RTT failed for target={} host={} port={}",
                        target.getRegion(),
                        target.getHost(),
                        target.getPort());

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.WEBSOCKET_RTT);
                result.setValueMs(-1);
                result.setSuccess(false);
                result.setTimestamp(Instant.now());
                result.setExtraTag(null);

                centralApiClient.send(result);
            }
        }
    }
}