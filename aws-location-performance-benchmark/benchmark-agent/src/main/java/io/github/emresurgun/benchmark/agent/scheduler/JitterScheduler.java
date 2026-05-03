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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;

@Component
public class JitterScheduler {

    private final AgentProperties agentProperties;
    private final CentralApiClient centralApiClient;

    private static final Logger log = LoggerFactory.getLogger(JitterScheduler.class);

    public JitterScheduler(AgentProperties agentProperties, CentralApiClient centralApiClient) {
        this.agentProperties = agentProperties;
        this.centralApiClient = centralApiClient;
    }

    @Scheduled(fixedDelay = 30000)
    public void measureJitter() {
        for (TargetConfig target : agentProperties.getTargets()) {
            try {
                double previousLatencyMs = measureSingleTcpLatency(target);
                double totalDifferenceMs = 0.0;
                int differenceCount = 0;

                for (int i = 0; i < 4; i++) {
                    double currentLatencyMs = measureSingleTcpLatency(target);

                    double differenceMs = Math.abs(currentLatencyMs - previousLatencyMs);
                    totalDifferenceMs += differenceMs;
                    differenceCount++;

                    previousLatencyMs = currentLatencyMs;
                }

                double jitterMs = totalDifferenceMs / differenceCount;

                log.info("Jitter target={} valueMs={}",
                        target.getRegion(),
                        jitterMs);

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.JITTER);
                result.setValueMs(jitterMs);
                result.setSuccess(true);
                result.setTimestamp(Instant.now());
                result.setExtraTag("sampleCount=5");

                centralApiClient.send(result);

            } catch (IOException e) {
                log.warn("Jitter failed for target={} host={} port={}",
                        target.getRegion(),
                        target.getHost(),
                        target.getPort());

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.JITTER);
                result.setValueMs(-1);
                result.setSuccess(false);
                result.setTimestamp(Instant.now());
                result.setExtraTag("sampleCount=5");

                centralApiClient.send(result);
            }
        }
    }

    private double measureSingleTcpLatency(TargetConfig target) throws IOException {
        try (Socket socket = new Socket()) {
            long start = System.nanoTime();

            socket.connect(
                    new InetSocketAddress(target.getHost(), target.getPort()),
                    3000
            );

            long elapsedNanos = System.nanoTime() - start;
            return elapsedNanos / 1_000_000.0;
        }
    }
}