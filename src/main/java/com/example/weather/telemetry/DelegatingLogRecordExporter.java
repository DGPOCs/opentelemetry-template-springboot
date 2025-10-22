package com.example.weather.telemetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

class DelegatingLogRecordExporter implements LogRecordExporter {

    private final List<LogRecordExporter> delegates;

    DelegatingLogRecordExporter(List<LogRecordExporter> delegates) {
        this.delegates = delegates;
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (LogRecordExporter delegate : delegates) {
            results.add(delegate.export(logs));
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode flush() {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (LogRecordExporter delegate : delegates) {
            results.add(delegate.flush());
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode shutdown() {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (LogRecordExporter delegate : delegates) {
            results.add(delegate.shutdown());
        }
        return CompletableResultCode.ofAll(results);
    }
}
