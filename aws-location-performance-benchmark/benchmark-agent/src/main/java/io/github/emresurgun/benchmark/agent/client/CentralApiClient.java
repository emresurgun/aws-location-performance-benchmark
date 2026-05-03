package io.github.emresurgun.benchmark.agent.client;

import io.github.emresurgun.benchmark.agent.model.MeasurementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import io.github.emresurgun.benchmark.agent.config.AgentProperties;
import org.springframework.web.client.RestClient;

@Component
public class CentralApiClient {

    private final AgentProperties agentProperties;
    private final RestClient restClient;

    public CentralApiClient(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
        this.restClient = RestClient.create();
    }

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
        restClient.post()
                .uri(agentProperties.getCentralApiUrl()+ "/api/measurements")
                .body(result)
                .retrieve()
                .toBodilessEntity();
    }

}
