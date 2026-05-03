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


@Component
public class TcpLatencyScheduler {
    private final AgentProperties agentProperties;

    private final CentralApiClient centralApiClient;

    public TcpLatencyScheduler(AgentProperties agentProperties, CentralApiClient centralApiClient)
    {
        this.agentProperties=agentProperties;
        this.centralApiClient=centralApiClient;
    }

    private static final Logger log = LoggerFactory.getLogger(TcpLatencyScheduler.class);

    @Scheduled(fixedDelay = 30000)
    public void measureTcpLatency()
    {
        for(TargetConfig target: agentProperties.getTargets())
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
}
