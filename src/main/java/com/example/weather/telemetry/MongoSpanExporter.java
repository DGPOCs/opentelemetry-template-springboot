package com.example.weather.telemetry;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

class MongoSpanExporter implements SpanExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoSpanExporter.class);

    private final MongoCollection<Document> collection;

    MongoSpanExporter(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (spans == null || spans.isEmpty()) {
            return CompletableResultCode.ofSuccess();
        }
        try {
            List<Document> documents = spans.stream()
                .map(this::toDocument)
                .collect(Collectors.toList());
            collection.insertMany(documents);
            return CompletableResultCode.ofSuccess();
        } catch (Exception exception) {
            LOGGER.error("Failed to persist spans to MongoDB", exception);
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    private Document toDocument(SpanData span) {
        Document document = new Document();
        document.append("traceId", span.getTraceId());
        document.append("spanId", span.getSpanId());
        document.append("parentSpanId", span.getParentSpanId());
        document.append("name", span.getName());
        document.append("kind", span.getKind().name());
        document.append("start", TelemetryDocumentFactory.createInstantDocument(span.getStartEpochNanos()));
        document.append("end", TelemetryDocumentFactory.createInstantDocument(span.getEndEpochNanos()));
        document.append("status", span.getStatus().getStatusCode().name());
        document.append("attributes", TelemetryDocumentFactory.toDocument(span.getAttributes()));
        document.append("totalRecordedEvents", span.getTotalRecordedEvents());
        document.append("totalRecordedLinks", span.getTotalRecordedLinks());
        document.append("resource", TelemetryDocumentFactory.createResourceDocument(span.getResource()));
        document.append("instrumentationScope",
            TelemetryDocumentFactory.createInstrumentationScopeDocument(span.getInstrumentationScopeInfo()));
        if (!span.getEvents().isEmpty()) {
            document.append("events", span.getEvents().stream().map(this::toEventDocument).collect(Collectors.toList()));
        }
        if (!span.getLinks().isEmpty()) {
            document.append("links", span.getLinks().stream().map(link -> new Document()
                .append("traceId", link.getSpanContext().getTraceId())
                .append("spanId", link.getSpanContext().getSpanId())
                .append("attributes", TelemetryDocumentFactory.toDocument(link.getAttributes()))).collect(Collectors.toList()));
        }
        return document;
    }

    private Document toEventDocument(EventData event) {
        Document document = new Document();
        document.append("name", event.getName());
        document.append("time", TelemetryDocumentFactory.createInstantDocument(event.getEpochNanos()));
        document.append("attributes", TelemetryDocumentFactory.toDocument(event.getAttributes()));
        return document;
    }
}
