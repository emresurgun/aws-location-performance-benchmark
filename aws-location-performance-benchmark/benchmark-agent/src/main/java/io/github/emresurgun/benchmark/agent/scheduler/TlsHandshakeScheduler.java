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

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.time.Instant;

@Component
public class TlsHandshakeScheduler {

    private final AgentProperties agentProperties;
    private final CentralApiClient centralApiClient;

    private static final Logger log = LoggerFactory.getLogger(TlsHandshakeScheduler.class);

    public TlsHandshakeScheduler(AgentProperties agentProperties, CentralApiClient centralApiClient) {
        this.agentProperties = agentProperties;
        this.centralApiClient = centralApiClient;
    }

    @Scheduled(fixedDelay = 30000)
    public void measureTlsHandshake() {
        for (TargetConfig target : agentProperties.getTargets()) {
            try {
                log.info("Measuring TLS handshake for target={} host={} port={}",
                        target.getRegion(),
                        target.getHost(),
                        target.getPort());

                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

                long start = System.nanoTime();

                try (SSLSocket socket = (SSLSocket) factory.createSocket(target.getHost(), target.getPort())) {
                    socket.setSoTimeout(3000);
                    socket.startHandshake();
                }

                long elapsedNanos = System.nanoTime() - start;
                double valueMs = elapsedNanos / 1_000_000.0;

                log.info("TLS handshake target={} valueMs={}",
                        target.getRegion(),
                        valueMs);

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.TLS_HANDSHAKE);
                result.setValueMs(valueMs);
                result.setSuccess(true);
                result.setTimestamp(Instant.now());
                result.setExtraTag(null);

                centralApiClient.send(result);
            }
            catch (Exception e) {
                log.warn("TLS handshake failed for target={} host={} port={}",
                        target.getRegion(),
                        target.getHost(),
                        target.getPort());

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.TLS_HANDSHAKE);
                result.setValueMs(-1);
                result.setSuccess(false);
                result.setTimestamp(Instant.now());
                result.setExtraTag(null);

                centralApiClient.send(result);
            }
        }
    }
}