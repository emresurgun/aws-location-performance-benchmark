package io.github.emresurgun.benchmark.central.model;

import java.time.Instant;

public class MeasurementResult {

    private String sourceRegion;
    private String targetRegion;
    private MetricType metricType;
    private double valueMs;
    private boolean success;
    private Instant timestamp;
    private String extraTag;

    public String getExtraTag() {
        return extraTag;
    }

    public void setExtraTag(String extraTag) {
        this.extraTag = extraTag;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public double getValueMs() {
        return valueMs;
    }

    public void setValueMs(double valueMs) {
        this.valueMs = valueMs;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public String getTargetRegion() {
        return targetRegion;
    }

    public void setTargetRegion(String targetRegion) {
        this.targetRegion = targetRegion;
    }

    public String getSourceRegion() {
        return sourceRegion;
    }

    public void setSourceRegion(String sourceRegion) {
        this.sourceRegion = sourceRegion;
    }
}
