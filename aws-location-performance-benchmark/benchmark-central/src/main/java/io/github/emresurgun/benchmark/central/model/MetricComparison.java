package io.github.emresurgun.benchmark.central.model;

public class MetricComparison {

    private MetricType metricType;

    private String baselineTargetRegion;
    private String candidateTargetRegion;

    private double baselineAverageMs;
    private double candidateAverageMs;
    private double averageImprovementPercent;

    private double baselineP95Ms;
    private double candidateP95Ms;
    private double p95ImprovementPercent;

    private double baselineSuccessRate;
    private double candidateSuccessRate;
    private double successRateDifference;

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public String getBaselineTargetRegion() {
        return baselineTargetRegion;
    }

    public void setBaselineTargetRegion(String baselineTargetRegion) {
        this.baselineTargetRegion = baselineTargetRegion;
    }

    public String getCandidateTargetRegion() {
        return candidateTargetRegion;
    }

    public void setCandidateTargetRegion(String candidateTargetRegion) {
        this.candidateTargetRegion = candidateTargetRegion;
    }

    public double getBaselineAverageMs() {
        return baselineAverageMs;
    }

    public void setBaselineAverageMs(double baselineAverageMs) {
        this.baselineAverageMs = baselineAverageMs;
    }

    public double getCandidateAverageMs() {
        return candidateAverageMs;
    }

    public void setCandidateAverageMs(double candidateAverageMs) {
        this.candidateAverageMs = candidateAverageMs;
    }

    public double getAverageImprovementPercent() {
        return averageImprovementPercent;
    }

    public void setAverageImprovementPercent(double averageImprovementPercent) {
        this.averageImprovementPercent = averageImprovementPercent;
    }

    public double getBaselineP95Ms() {
        return baselineP95Ms;
    }

    public void setBaselineP95Ms(double baselineP95Ms) {
        this.baselineP95Ms = baselineP95Ms;
    }

    public double getCandidateP95Ms() {
        return candidateP95Ms;
    }

    public void setCandidateP95Ms(double candidateP95Ms) {
        this.candidateP95Ms = candidateP95Ms;
    }

    public double getP95ImprovementPercent() {
        return p95ImprovementPercent;
    }

    public void setP95ImprovementPercent(double p95ImprovementPercent) {
        this.p95ImprovementPercent = p95ImprovementPercent;
    }

    public double getBaselineSuccessRate() {
        return baselineSuccessRate;
    }

    public void setBaselineSuccessRate(double baselineSuccessRate) {
        this.baselineSuccessRate = baselineSuccessRate;
    }

    public double getCandidateSuccessRate() {
        return candidateSuccessRate;
    }

    public void setCandidateSuccessRate(double candidateSuccessRate) {
        this.candidateSuccessRate = candidateSuccessRate;
    }

    public double getSuccessRateDifference() {
        return successRateDifference;
    }

    public void setSuccessRateDifference(double successRateDifference) {
        this.successRateDifference = successRateDifference;
    }
}