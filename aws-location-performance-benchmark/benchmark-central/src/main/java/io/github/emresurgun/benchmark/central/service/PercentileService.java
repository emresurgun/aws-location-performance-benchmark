package io.github.emresurgun.benchmark.central.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PercentileService {

    public double percentile(List<Double> values, double percentile) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);

        if (sortedValues.size() == 1) {
            return sortedValues.get(0);
        }

        double index = percentile / 100.0 * (sortedValues.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }

        double lowerValue = sortedValues.get(lowerIndex);
        double upperValue = sortedValues.get(upperIndex);
        double weight = index - lowerIndex;

        return lowerValue + (upperValue - lowerValue) * weight;
    }
}