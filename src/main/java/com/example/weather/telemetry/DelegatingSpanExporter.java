package com.example.weather.telemetry;

import java.util.List;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

class DelegatingSpanExporter implements SpanExporter {

    private final List<SpanExporter> delegates;

    DelegatingSpanExporter(List<SpanExporter> delegates) {
        this.delegates = delegates;
    }

    @Override
    public CompletableResultCode export(java.util.Collection<SpanData> spans) {
        return CompletableResultCode.ofAll(delegates.stream()
            .map(delegate -> delegate.export(spans))
            .collect(Collectors.toList()));
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofAll(delegates.stream()
            .map(SpanExporter::flush)
            .collect(Collectors.toList()));
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofAll(delegates.stream()
            .map(SpanExporter::shutdown)
            .collect(Collectors.toList()));
    }
}
