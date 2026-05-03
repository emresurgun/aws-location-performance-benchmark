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
public class ThroughputScheduler {

    private final AgentProperties agentProperties;
    private final CentralApiClient centralApiClient;
    private final RestClient restClient;
    private static final Logger log = LoggerFactory.getLogger(ThroughputScheduler.class);

    public ThroughputScheduler(AgentProperties agentProperties, CentralApiClient centralApiClient)
    {
        this.agentProperties=agentProperties;
        this.centralApiClient=centralApiClient;
        this.restClient=RestClient.create();
    }

    @Scheduled(fixedDelay = 30000)
    public void measureDownloadThroughput()
    {
        for(TargetConfig target: agentProperties.getTargets())
        {
            try {
                String url = "http://" + target.getHost() + ":" + target.getPort() + "/download/1024";
                log.info("Measuring Download Throughput for target={} url={}",
                        target.getRegion(),
                        url);

                long start = System.nanoTime();

                byte[] data = restClient.get().uri(url).retrieve().body(byte[].class);

                long elapsedNanos = System.nanoTime() - start;
                double valueMs = elapsedNanos / 1_000_000.0;

                int bytes = data.length;

                log.info("Download throughput target={} valueMs={} bytes={}",
                        target.getRegion(),
                        valueMs,
                        bytes);

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.THROUGHPUT_DOWN);
                result.setValueMs(valueMs);
                result.setSuccess(true);
                result.setTimestamp(Instant.now());
                result.setExtraTag("sizeKb=1024");

                centralApiClient.send(result);
            }
            catch (Exception e)
            {
                log.warn("Download Throughput failed for target={} host={} port={}",
                        target.getRegion(),
                        target.getHost(),
                        target.getPort());

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.THROUGHPUT_DOWN);
                result.setValueMs(-1);
                result.setSuccess(false);
                result.setTimestamp(Instant.now());
                result.setExtraTag("sizeKb=1024");
                centralApiClient.send(result);
            }
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void measureUploadThroughput()
    {
        byte[] uploadData = new byte[1024 * 1024];


        for(TargetConfig target: agentProperties.getTargets())
        {
            try {


                String url = "http://" + target.getHost() + ":" + target.getPort() + "/upload";

                log.info("Measuring Upload Throughput for target={} url={}",
                        target.getRegion(),
                        url);

                long start = System.nanoTime();

                restClient.post().uri(url).body(uploadData).retrieve().toBodilessEntity();

                long elapsedNanos = System.nanoTime() - start;
                double valueMs = elapsedNanos / 1_000_000.0;

                log.info("Upload throughput target={} valueMs={} bytes={}",
                        target.getRegion(),
                        valueMs,
                        uploadData.length);

                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.THROUGHPUT_UP);
                result.setValueMs(valueMs);
                result.setSuccess(true);
                result.setTimestamp(Instant.now());
                result.setExtraTag("sizeKb=1024");
                centralApiClient.send(result);
            }catch (Exception e)
            {
                log.warn("Upload Throughput failed for target={} host={} port={}",
                        target.getRegion(),
                        target.getHost(),
                        target.getPort());
                MeasurementResult result = new MeasurementResult();
                result.setSourceRegion(agentProperties.getSourceRegion());
                result.setTargetRegion(target.getRegion());
                result.setMetricType(MetricType.THROUGHPUT_UP);
                result.setValueMs(-1);
                result.setSuccess(false);
                result.setTimestamp(Instant.now());
                result.setExtraTag("sizeKb=1024");
                centralApiClient.send(result);

            }

        }
    }

}
