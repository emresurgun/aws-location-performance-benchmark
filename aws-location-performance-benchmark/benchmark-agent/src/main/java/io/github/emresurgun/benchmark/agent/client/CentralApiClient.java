package io.github.emresurgun.benchmark.agent.client;

import io.github.emresurgun.benchmark.agent.model.MeasurementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CentralApiClient {
    private static final Logger log = LoggerFactory.getLogger(CentralApiClient.class);

    public void send(MeasurementResult result)
    {
        log.info(
                "Measurement result: target={}, metric={}, valueMs={}, success={}",
                result.getTargetRegion(),
                result.getMetricType(),
                result.getValueMs(),
                result.isSuccess()
        );
    }

}
