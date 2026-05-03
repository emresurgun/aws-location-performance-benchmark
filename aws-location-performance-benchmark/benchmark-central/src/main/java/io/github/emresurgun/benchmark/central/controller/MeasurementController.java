package io.github.emresurgun.benchmark.central.controller;

import io.github.emresurgun.benchmark.central.model.MeasurementResult;
import io.github.emresurgun.benchmark.central.service.InfluxWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/measurements")
public class MeasurementController {

    private static final Logger log = LoggerFactory.getLogger(MeasurementController.class);

    private final InfluxWriteService influxWriteService;

    public MeasurementController(InfluxWriteService influxWriteService) {
        this.influxWriteService = influxWriteService;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody MeasurementResult result) {
        log.info(
                "Received measurement: source={}, target={}, metric={}, valueMs={}, success={}",
                result.getSourceRegion(),
                result.getTargetRegion(),
                result.getMetricType(),
                result.getValueMs(),
                result.isSuccess()
        );

        influxWriteService.write(result);

        return ResponseEntity.ok().build();
    }
}