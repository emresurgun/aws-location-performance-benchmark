package io.github.emresurgun.benchmark.central.model;

public class MetricSummary {

    private MetricType metricType;
    private String targetRegion;

    private long sampleCount;
    private long successCount;
    private long failureCount;

    private double averageMs;
    private double minMs;
    private double maxMs;

    private double p50Ms;
    private double p95Ms;
    private double p99Ms;

    private double successRate;

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

    public long getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(long sampleCount) {
        this.sampleCount = sampleCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(long failureCount) {
        this.failureCount = failureCount;
    }

    public double getAverageMs() {
        return averageMs;
    }

    public void setAverageMs(double averageMs) {
        this.averageMs = averageMs;
    }

    public double getMinMs() {
        return minMs;
    }

    public void setMinMs(double minMs) {
        this.minMs = minMs;
    }

    public double getMaxMs() {
        return maxMs;
    }

    public void setMaxMs(double maxMs) {
        this.maxMs = maxMs;
    }

    public double getP50Ms() {
        return p50Ms;
    }

    public void setP50Ms(double p50Ms) {
        this.p50Ms = p50Ms;
    }

    public double getP95Ms() {
        return p95Ms;
    }

    public void setP95Ms(double p95Ms) {
        this.p95Ms = p95Ms;
    }

    public double getP99Ms() {
        return p99Ms;
    }

    public void setP99Ms(double p99Ms) {
        this.p99Ms = p99Ms;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
}