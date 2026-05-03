package io.github.emresurgun.benchmark.central.service;

import com.influxdb.client.InfluxDBClient;

import com.influxdb.query.FluxRecord;

import com.influxdb.query.FluxTable;

import io.github.emresurgun.benchmark.central.model.MeasurementResult;

import io.github.emresurgun.benchmark.central.model.MetricSummary;

import io.github.emresurgun.benchmark.central.model.MetricType;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.time.Instant;

import java.util.ArrayList;

import java.util.DoubleSummaryStatistics;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

@Service

public class InfluxQueryService {

    private final InfluxDBClient influxDBClient;

    private final PercentileService percentileService;

    @Value("${influx.bucket}")

    private String bucket;

    public InfluxQueryService(InfluxDBClient influxDBClient, PercentileService percentileService) {

        this.influxDBClient = influxDBClient;

        this.percentileService = percentileService;

    }

    public List<MeasurementResult> findRecentMeasurements() {

        String flux = """

                from(bucket: "%s")

                  |> range(start: -1h)

                  |> filter(fn: (r) => r["_measurement"] == "bench_metric")

                  |> filter(fn: (r) => r["_field"] == "value_ms" or r["_field"] == "success")

                  |> pivot(rowKey: ["_time", "source", "target", "metric"], columnKey: ["_field"], valueColumn: "_value")

                  |> sort(columns: ["_time"], desc: true)

                  |> limit(n: 100)

                """.formatted(bucket);

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);

        List<MeasurementResult> results = new ArrayList<>();

        for (FluxTable table : tables) {

            for (FluxRecord record : table.getRecords()) {

                MeasurementResult result = mapRecordToMeasurementResult(record);

                results.add(result);

            }

        }

        return results;

    }

    public List<MetricSummary> findMetricSummaries() {

        String flux = """

                from(bucket: "%s")

                  |> range(start: -1h)

                  |> filter(fn: (r) => r["_measurement"] == "bench_metric")

                  |> filter(fn: (r) => r["_field"] == "value_ms" or r["_field"] == "success")

                  |> pivot(rowKey: ["_time", "source", "target", "metric"], columnKey: ["_field"], valueColumn: "_value")

                """.formatted(bucket);

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);

        Map<String, List<MeasurementResult>> groupedMeasurements = new HashMap<>();

        for (FluxTable table : tables) {

            for (FluxRecord record : table.getRecords()) {

                MeasurementResult result = mapRecordToMeasurementResult(record);

                String key = result.getTargetRegion() + "|" + result.getMetricType().name();

                groupedMeasurements

                        .computeIfAbsent(key, ignored -> new ArrayList<>())

                        .add(result);

            }

        }

        List<MetricSummary> summaries = new ArrayList<>();

        for (List<MeasurementResult> group : groupedMeasurements.values()) {

            summaries.add(buildSummary(group));

        }

        return summaries;

    }

    private MeasurementResult mapRecordToMeasurementResult(FluxRecord record) {

        MeasurementResult result = new MeasurementResult();

        result.setSourceRegion((String) record.getValueByKey("source"));

        result.setTargetRegion((String) record.getValueByKey("target"));

        result.setMetricType(MetricType.valueOf((String) record.getValueByKey("metric")));

        Object valueMs = record.getValueByKey("value_ms");

        if (valueMs instanceof Number number) {

            result.setValueMs(number.doubleValue());

        }

        Object success = record.getValueByKey("success");

        if (success instanceof Boolean booleanValue) {

            result.setSuccess(booleanValue);

        }

        if (record.getTime() != null) {

            result.setTimestamp(record.getTime());

        } else {

            result.setTimestamp(Instant.now());

        }

        Object extra = record.getValueByKey("extra");

        if (extra instanceof String extraValue) {

            result.setExtraTag(extraValue);

        }

        return result;

    }

    private MetricSummary buildSummary(List<MeasurementResult> measurements) {

        MetricSummary summary = new MetricSummary();

        if (measurements == null || measurements.isEmpty()) {

            return summary;

        }

        MeasurementResult first = measurements.get(0);

        summary.setTargetRegion(first.getTargetRegion());

        summary.setMetricType(first.getMetricType());

        summary.setSampleCount(measurements.size());

        long successCount = measurements.stream()

                .filter(MeasurementResult::isSuccess)

                .count();

        long failureCount = measurements.size() - successCount;

        summary.setSuccessCount(successCount);

        summary.setFailureCount(failureCount);

        double successRate = (successCount * 100.0) / measurements.size();

        summary.setSuccessRate(successRate);

        List<Double> successfulValues = measurements.stream()

                .filter(MeasurementResult::isSuccess)

                .map(MeasurementResult::getValueMs)

                .toList();

        if (successfulValues.isEmpty()) {

            summary.setAverageMs(0.0);

            summary.setMinMs(0.0);

            summary.setMaxMs(0.0);

            summary.setP50Ms(0.0);

            summary.setP95Ms(0.0);

            summary.setP99Ms(0.0);

            return summary;

        }

        DoubleSummaryStatistics statistics = successfulValues.stream()

                .mapToDouble(Double::doubleValue)

                .summaryStatistics();

        summary.setAverageMs(statistics.getAverage());

        summary.setMinMs(statistics.getMin());

        summary.setMaxMs(statistics.getMax());

        summary.setP50Ms(percentileService.percentile(successfulValues, 50));

        summary.setP95Ms(percentileService.percentile(successfulValues, 95));

        summary.setP99Ms(percentileService.percentile(successfulValues, 99));

        return summary;

    }

}