package com.example.weather.telemetry;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

class MongoLogRecordExporter implements LogRecordExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoLogRecordExporter.class);

    private final MongoCollection<Document> collection;

    MongoLogRecordExporter(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        if (logs == null || logs.isEmpty()) {
            return CompletableResultCode.ofSuccess();
        }
        try {
            List<Document> documents = logs.stream()
                .map(this::toDocument)
                .collect(Collectors.toList());
            collection.insertMany(documents);
            return CompletableResultCode.ofSuccess();
        } catch (Exception exception) {
            LOGGER.error("Failed to persist logs to MongoDB", exception);
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

    private Document toDocument(LogRecordData log) {
        Document document = new Document();
        document.append("observedTime", TelemetryDocumentFactory.createInstantDocument(log.getObservedTimestampEpochNanos()));
        document.append("timestamp", TelemetryDocumentFactory.createInstantDocument(log.getTimestampEpochNanos()));
        document.append("severityText", log.getSeverityText());
        document.append("severityNumber", log.getSeverity() != null ? log.getSeverity().getSeverityNumber() : null);
        document.append("body", log.getBody().asString());
        document.append("attributes", TelemetryDocumentFactory.toDocument(log.getAttributes()));
        document.append("resource", TelemetryDocumentFactory.createResourceDocument(log.getResource()));
        document.append("instrumentationScope",
            TelemetryDocumentFactory.createInstrumentationScopeDocument(log.getInstrumentationScopeInfo()));
        document.append("spanContext", new Document()
            .append("traceId", log.getSpanContext().getTraceId())
            .append("spanId", log.getSpanContext().getSpanId())
            .append("traceFlags", log.getSpanContext().getTraceFlags().asHex())
            .append("traceState", log.getSpanContext().getTraceState().asMap()));
        return document;
    }
}
