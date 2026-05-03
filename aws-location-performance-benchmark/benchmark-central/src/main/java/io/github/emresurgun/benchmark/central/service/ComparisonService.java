package io.github.emresurgun.benchmark.central.service;

import io.github.emresurgun.benchmark.central.model.MetricComparison;
import io.github.emresurgun.benchmark.central.model.MetricSummary;
import io.github.emresurgun.benchmark.central.model.MetricType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ComparisonService {

    public List<MetricComparison> compare(
            List<MetricSummary> summaries,
            String baselineTargetRegion,
            String candidateTargetRegion
    ) {
        List<MetricComparison> comparisons = new ArrayList<>();

        for (MetricType metricType : MetricType.values()) {
            Optional<MetricSummary> baselineSummary = findSummary(
                    summaries,
                    metricType,
                    baselineTargetRegion
            );

            Optional<MetricSummary> candidateSummary = findSummary(
                    summaries,
                    metricType,
                    candidateTargetRegion
            );

            if (baselineSummary.isEmpty() || candidateSummary.isEmpty()) {
                continue;
            }

            MetricComparison comparison = buildComparison(
                    baselineSummary.get(),
                    candidateSummary.get()
            );

            comparisons.add(comparison);
        }

        return comparisons;
    }

    private Optional<MetricSummary> findSummary(
            List<MetricSummary> summaries,
            MetricType metricType,
            String targetRegion
    ) {
        return summaries.stream()
                .filter(summary -> summary.getMetricType() == metricType)
                .filter(summary -> targetRegion.equals(summary.getTargetRegion()))
                .findFirst();
    }

    private MetricComparison buildComparison(
            MetricSummary baseline,
            MetricSummary candidate
    ) {
        MetricComparison comparison = new MetricComparison();

        comparison.setMetricType(baseline.getMetricType());

        comparison.setBaselineTargetRegion(baseline.getTargetRegion());
        comparison.setCandidateTargetRegion(candidate.getTargetRegion());

        comparison.setBaselineAverageMs(baseline.getAverageMs());
        comparison.setCandidateAverageMs(candidate.getAverageMs());
        comparison.setAverageImprovementPercent(
                calculateLowerIsBetterImprovement(
                        baseline.getAverageMs(),
                        candidate.getAverageMs()
                )
        );

        comparison.setBaselineP95Ms(baseline.getP95Ms());
        comparison.setCandidateP95Ms(candidate.getP95Ms());
        comparison.setP95ImprovementPercent(
                calculateLowerIsBetterImprovement(
                        baseline.getP95Ms(),
                        candidate.getP95Ms()
                )
        );

        comparison.setBaselineSuccessRate(baseline.getSuccessRate());
        comparison.setCandidateSuccessRate(candidate.getSuccessRate());
        comparison.setSuccessRateDifference(
                candidate.getSuccessRate() - baseline.getSuccessRate()
        );

        return comparison;
    }

    private double calculateLowerIsBetterImprovement(
            double baselineValue,
            double candidateValue
    ) {
        if (baselineValue <= 0) {
            return 0.0;
        }

        return ((baselineValue - candidateValue) / baselineValue) * 100.0;
    }
}