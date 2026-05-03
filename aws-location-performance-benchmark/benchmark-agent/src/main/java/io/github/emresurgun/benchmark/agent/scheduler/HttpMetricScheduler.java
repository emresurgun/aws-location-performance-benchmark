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
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class HttpMetricScheduler {

    private final AgentProperties agentProperties;
    private final CentralApiClient centralApiClient;
    private final RestClient restClient;

    public HttpMetricScheduler(AgentProperties agentProperties, CentralApiClient centralApiClient)
    {
        this.agentProperties=agentProperties;
        this.centralApiClient=centralApiClient;
        this.restClient=RestClient.create();
    }

    private static final Logger log = LoggerFactory.getLogger(HttpMetricScheduler.class);



    @Scheduled(fixedDelay = 30000)
    public void measureHttpTtfb()
    {
        for(TargetConfig target: agentProperties.getTargets())
        {
            try{
                String url = "http://" + target.getHost() + ":" + target.getPort() + "/ping";
                log.info("Measuring HTTP TTFB for target={} url={}",
                        target.getRegion(),
                        url);

                long start = System.nanoTime();

                restClient.get().uri(url).retrieve().body(String.class);

                long elapsedNanos = System.nanoTime() - start;
                double valueMs = elapsedNanos / 1_000_000.0;

                log.info("HTTP TTFB target={} valueMs={}",
                        target.getRegion(),
                        valueMs);

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.HTTP_TTFB);
                result.setValueMs(valueMs);
                result.setSuccess(true);
                result.setTimestamp(Instant.now());
                result.setExtraTag(null);

                centralApiClient.send(result);
            }
            catch (Exception e)
            {
                log.warn("HTTP TTFB failed for target={} host={} port={}",
                        target.getRegion(),
                        target.getHost(),
                        target.getPort());
                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.HTTP_TTFB);
                result.setValueMs(-1);
                result.setSuccess(false);
                result.setTimestamp(Instant.now());
                result.setExtraTag(null);
                centralApiClient.send(result);
            }

        }
    }




}
