package io.github.emresurgun.benchmark.central.controller;

import io.github.emresurgun.benchmark.central.model.MeasurementResult;
import io.github.emresurgun.benchmark.central.model.MetricComparison;
import io.github.emresurgun.benchmark.central.model.MetricSummary;
import io.github.emresurgun.benchmark.central.service.ComparisonService;
import io.github.emresurgun.benchmark.central.service.InfluxQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final InfluxQueryService influxQueryService;
    private final ComparisonService comparisonService;

    public MetricsController(
            InfluxQueryService influxQueryService,
            ComparisonService comparisonService
    ) {
        this.influxQueryService = influxQueryService;
        this.comparisonService = comparisonService;
    }

    @GetMapping("/recent")
    public List<MeasurementResult> getRecentMeasurements() {
        return influxQueryService.findRecentMeasurements();
    }

    @GetMapping("/summary")
    public List<MetricSummary> getMetricSummaries() {
        return influxQueryService.findMetricSummaries();
    }

    @GetMapping("/comparison")
    public List<MetricComparison> getMetricComparisons(
            @RequestParam(defaultValue = "frankfurt-local-test") String baseline,
            @RequestParam(defaultValue = "istanbul-local-test") String candidate
    ) {
        List<MetricSummary> summaries = influxQueryService.findMetricSummaries();

        return comparisonService.compare(
                summaries,
                baseline,
                candidate
        );
    }
}