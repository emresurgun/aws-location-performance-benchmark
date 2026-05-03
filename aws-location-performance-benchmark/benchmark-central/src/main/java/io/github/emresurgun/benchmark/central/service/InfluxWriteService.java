package io.github.emresurgun.benchmark.central.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import io.github.emresurgun.benchmark.central.model.MeasurementResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class InfluxWriteService {

    private final InfluxDBClient influxDBClient;

    @Value("${influx.bucket}")
    private String bucket;

    @Value("${influx.org}")
    private String org;

    public InfluxWriteService(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    public void write(MeasurementResult result) {
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        Point point = Point.measurement("bench_metric")
                .addTag("source", safe(result.getSourceRegion()))
                .addTag("target", safe(result.getTargetRegion()))
                .addTag("metric", result.getMetricType().name())
                .addField("value_ms", result.getValueMs())
                .addField("success", result.isSuccess())
                .time(resolveTimestamp(result), WritePrecision.MS);

        if (result.getExtraTag() != null) {
            point.addTag("extra", result.getExtraTag());
        }

        writeApi.writePoint(bucket, org, point);
    }

    private Instant resolveTimestamp(MeasurementResult result) {
        if (result.getTimestamp() == null) {
            return Instant.now();
        }

        return result.getTimestamp();
    }

    private String safe(String value) {
        if (value == null) {
            return "unknown";
        }

        return value;
    }
}