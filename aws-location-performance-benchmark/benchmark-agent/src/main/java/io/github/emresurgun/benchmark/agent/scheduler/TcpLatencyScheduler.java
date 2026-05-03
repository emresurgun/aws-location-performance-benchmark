package io.github.emresurgun.benchmark.agent.scheduler;

import io.github.emresurgun.benchmark.agent.model.TargetConfig;
import io.github.emresurgun.benchmark.agent.client.CentralApiClient;
import io.github.emresurgun.benchmark.agent.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.github.emresurgun.benchmark.agent.model.MeasurementResult;
import io.github.emresurgun.benchmark.agent.model.MetricType;

import java.time.Instant;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Component
public class TcpLatencyScheduler {

    private final AgentProperties agentProperties;
    private final CentralApiClient centralApiClient;

    private final Map<String, Double> lastTcpLatencyByTarget = new HashMap<>();
    private final Map<String, Double> jitterByTarget = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(TcpLatencyScheduler.class);

    public TcpLatencyScheduler(AgentProperties agentProperties, CentralApiClient centralApiClient)
    {
        this.agentProperties = agentProperties;
        this.centralApiClient = centralApiClient;
    }

    @Scheduled(fixedDelay = 30000)
    public void measureTcpLatency()
    {
        for (TargetConfig target : agentProperties.getTargets())
        {
            try (Socket socket = new Socket()) {
                long start = System.nanoTime();

                socket.connect(
                        new InetSocketAddress(target.getHost(), target.getPort()),
                        3000
                );

                long elapsedNanos = System.nanoTime() - start;
                double valueMs = elapsedNanos / 1_000_000.0;

                log.info("TCP latency target={} valueMs={}",
                        target.getRegion(),
                        valueMs);

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.TCP_LATENCY);
                result.setValueMs(valueMs);
                result.setSuccess(true);
                result.setTimestamp(Instant.now());
                result.setExtraTag(null);

                centralApiClient.send(result);

                calculateAndSendJitter(target, valueMs);

            } catch (IOException e) {
                log.warn("TCP latency failed for target={} host={} port={}",
                        target.getRegion(),
                        target.getHost(),
                        target.getPort());

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.TCP_LATENCY);
                result.setValueMs(-1);
                result.setSuccess(false);
                result.setTimestamp(Instant.now());
                result.setExtraTag(null);

                centralApiClient.send(result);
            }
        }
    }

    private void calculateAndSendJitter(TargetConfig target, double currentLatencyMs)
    {
        String targetKey = target.getRegion();

        Double previousLatencyMs = lastTcpLatencyByTarget.get(targetKey);

        if (previousLatencyMs != null) {
            double previousJitterMs = jitterByTarget.getOrDefault(targetKey, 0.0);

            double differenceMs = Math.abs(currentLatencyMs - previousLatencyMs);

            double jitterMs = previousJitterMs + ((differenceMs - previousJitterMs) / 16.0);

            jitterByTarget.put(targetKey, jitterMs);

            log.info("Jitter target={} valueMs={}",
                    target.getRegion(),
                    jitterMs);

            MeasurementResult jitterResult = new MeasurementResult();
            jitterResult.setSourceRegion(agentProperties.getSourceRegion());
            jitterResult.setTargetRegion(target.getRegion());
            jitterResult.setMetricType(MetricType.JITTER);
            jitterResult.setValueMs(jitterMs);
            jitterResult.setSuccess(true);
            jitterResult.setTimestamp(Instant.now());
            jitterResult.setExtraTag("sourceMetric=TCP_LATENCY");

            centralApiClient.send(jitterResult);
        }

        lastTcpLatencyByTarget.put(targetKey, currentLatencyMs);
    }
}