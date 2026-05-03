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
public class PacketLossScheduler {

    private final AgentProperties agentProperties;
    private final CentralApiClient centralApiClient;

    private static final Logger log = LoggerFactory.getLogger(PacketLossScheduler.class);

    public PacketLossScheduler(AgentProperties agentProperties, CentralApiClient centralApiClient) {
        this.agentProperties = agentProperties;
        this.centralApiClient = centralApiClient;
    }

    @Scheduled(fixedDelay = 60000)
    public void measurePacketLoss() {
        int totalAttempts = 100;

        for (TargetConfig target : agentProperties.getTargets()) {
            int failedAttempts = 0;

            for (int i = 0; i < totalAttempts; i++) {
                try (Socket socket = new Socket()) {
                    socket.connect(
                            new InetSocketAddress(target.getHost(), target.getPort()),
                            1000
                    );
                } catch (IOException e) {
                    failedAttempts++;
                }
            }

            double packetLossPercent = failedAttempts * 100.0 / totalAttempts;

            log.info("Packet loss target={} failedAttempts={} totalAttempts={} lossPercent={}",
                    target.getRegion(),
                    failedAttempts,
                    totalAttempts,
                    packetLossPercent);

            MeasurementResult result = new MeasurementResult();
            result.setSourceRegion(agentProperties.getSourceRegion());
            result.setTargetRegion(target.getRegion());
            result.setMetricType(MetricType.PACKET_LOSS);
            result.setValueMs(packetLossPercent);
            result.setSuccess(true);
            result.setTimestamp(Instant.now());
            result.setExtraTag("attempts=" + totalAttempts);

            centralApiClient.send(result);
        }
    }
}