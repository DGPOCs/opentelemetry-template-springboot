package com.example.weather.telemetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

class DelegatingMetricExporter implements MetricExporter {

    private final List<MetricExporter> delegates;

    DelegatingMetricExporter(List<MetricExporter> delegates) {
        this.delegates = delegates;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            results.add(delegate.export(metrics));
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode flush() {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            results.add(delegate.flush());
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode shutdown() {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            results.add(delegate.shutdown());
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        for (MetricExporter delegate : delegates) {
            AggregationTemporality temporality = delegate.getAggregationTemporality(instrumentType);
            if (temporality != null) {
                return temporality;
            }
        }
        return AggregationTemporality.CUMULATIVE;
    }
}
