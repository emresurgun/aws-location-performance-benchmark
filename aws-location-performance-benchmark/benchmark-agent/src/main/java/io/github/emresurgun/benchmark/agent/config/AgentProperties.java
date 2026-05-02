package io.github.emresurgun.benchmark.agent.config;

import io.github.emresurgun.benchmark.agent.model.TargetConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    private String sourceRegion;
    private String centralApiUrl;
    private List<TargetConfig> targets;

    public List<TargetConfig> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetConfig> targets) {
        this.targets = targets;
    }

    public String getCentralApiUrl() {
        return centralApiUrl;
    }

    public void setCentralApiUrl(String centralApiUrl) {
        this.centralApiUrl = centralApiUrl;
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public void setSourceRegion(String sourceRegion) {
        this.sourceRegion = sourceRegion;
    }
}
