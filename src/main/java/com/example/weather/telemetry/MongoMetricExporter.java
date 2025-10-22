package com.example.weather.telemetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.ExemplarData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.data.SummaryData;
import io.opentelemetry.sdk.metrics.data.SummaryPointData;
import io.opentelemetry.sdk.metrics.data.ValueAtQuantile;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

class MongoMetricExporter implements MetricExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoMetricExporter.class);

    private final MongoCollection<Document> collection;

    MongoMetricExporter(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return CompletableResultCode.ofSuccess();
        }
        try {
            List<Document> documents = new ArrayList<>();
            for (MetricData metric : metrics) {
                documents.addAll(toDocuments(metric));
            }
            if (!documents.isEmpty()) {
                collection.insertMany(documents);
            }
            return CompletableResultCode.ofSuccess();
        } catch (Exception exception) {
            LOGGER.error("Failed to persist metrics to MongoDB", exception);
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

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.CUMULATIVE;
    }

    private List<Document> toDocuments(MetricData metric) {
        List<Document> documents = new ArrayList<>();
        MetricDataType type = metric.getType();
        switch (type) {
            case LONG_GAUGE -> mapLongGauge(metric, documents);
            case DOUBLE_GAUGE -> mapDoubleGauge(metric, documents);
            case LONG_SUM, DOUBLE_SUM -> appendSumDocuments(metric, (SumData<?>) metric.getData(), documents);
            case HISTOGRAM -> mapHistogram(metric, documents);
            case EXPONENTIAL_HISTOGRAM -> mapExponentialHistogram(metric, documents);
            case SUMMARY -> mapSummary(metric, documents);
        }
        return documents;
    }

    private void mapLongGauge(MetricData metric, List<Document> documents) {
        @SuppressWarnings("unchecked")
        GaugeData<LongPointData> data = (GaugeData<LongPointData>) metric.getData();
        for (LongPointData point : data.getPoints()) {
            documents.add(createPointDocument(metric, point).append("value", point.getValue()));
        }
    }

    private void mapDoubleGauge(MetricData metric, List<Document> documents) {
        @SuppressWarnings("unchecked")
        GaugeData<DoublePointData> data = (GaugeData<DoublePointData>) metric.getData();
        for (DoublePointData point : data.getPoints()) {
            documents.add(createPointDocument(metric, point).append("value", point.getValue()));
        }
    }

    private void appendSumDocuments(MetricData metric, SumData<? extends PointData> sumData, List<Document> documents) {
        for (PointData point : sumData.getPoints()) {
            Document document = createPointDocument(metric, point)
                .append("value", extractNumericValue(point))
                .append("monotonic", sumData.isMonotonic())
                .append("temporality", sumData.getAggregationTemporality().name());
            documents.add(document);
        }
    }

    private void mapHistogram(MetricData metric, List<Document> documents) {
        HistogramData data = (HistogramData) metric.getData();
        for (HistogramPointData point : data.getPoints()) {
            documents.add(createPointDocument(metric, point).append("value", mapHistogram(point)));
        }
    }

    private void mapExponentialHistogram(MetricData metric, List<Document> documents) {
        ExponentialHistogramData data = (ExponentialHistogramData) metric.getData();
        for (ExponentialHistogramPointData point : data.getPoints()) {
            documents.add(createPointDocument(metric, point).append("value", mapExponentialHistogram(point)));
        }
    }

    private void mapSummary(MetricData metric, List<Document> documents) {
        SummaryData data = (SummaryData) metric.getData();
        for (SummaryPointData point : data.getPoints()) {
            documents.add(createPointDocument(metric, point).append("value", mapSummary(point)));
        }
    }

    private Number extractNumericValue(PointData point) {
        if (point instanceof LongPointData longPointData) {
            return longPointData.getValue();
        }
        if (point instanceof DoublePointData doublePointData) {
            return doublePointData.getValue();
        }
        return null;
    }

    private Map<String, Object> mapHistogram(HistogramPointData histogram) {
        Map<String, Object> document = new HashMap<>();
        document.put("sum", histogram.getSum());
        document.put("count", histogram.getCount());
        if (histogram.hasMin()) {
            document.put("min", histogram.getMin());
        }
        if (histogram.hasMax()) {
            document.put("max", histogram.getMax());
        }
        document.put("boundaries", histogram.getBoundaries());
        document.put("counts", histogram.getCounts());
        return document;
    }

    private Map<String, Object> mapExponentialHistogram(ExponentialHistogramPointData histogram) {
        Map<String, Object> document = new HashMap<>();
        document.put("scale", histogram.getScale());
        document.put("sum", histogram.getSum());
        document.put("count", histogram.getCount());
        document.put("zeroCount", histogram.getZeroCount());
        if (histogram.hasMin()) {
            document.put("min", histogram.getMin());
        }
        if (histogram.hasMax()) {
            document.put("max", histogram.getMax());
        }
        document.put("positive", mapExponentialBuckets(histogram.getPositiveBuckets()));
        document.put("negative", mapExponentialBuckets(histogram.getNegativeBuckets()));
        return document;
    }

    private Map<String, Object> mapExponentialBuckets(ExponentialHistogramBuckets buckets) {
        Map<String, Object> map = new HashMap<>();
        map.put("scale", buckets.getScale());
        map.put("offset", buckets.getOffset());
        map.put("counts", buckets.getBucketCounts());
        map.put("totalCount", buckets.getTotalCount());
        return map;
    }

    private Map<String, Object> mapSummary(SummaryPointData point) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("sum", point.getSum());
        summary.put("count", point.getCount());
        summary.put("quantiles", point.getValues().stream().map(this::mapQuantile).toList());
        return summary;
    }

    private Map<String, Object> mapQuantile(ValueAtQuantile quantile) {
        Map<String, Object> document = new HashMap<>();
        document.put("quantile", quantile.getQuantile());
        document.put("value", quantile.getValue());
        return document;
    }

    private Document createPointDocument(MetricData metric, PointData point) {
        Document document = new Document();
        document.append("name", metric.getName());
        document.append("description", metric.getDescription());
        document.append("unit", metric.getUnit());
        document.append("type", metric.getType().name());
        document.append("start", TelemetryDocumentFactory.createInstantDocument(point.getStartEpochNanos()));
        document.append("end", TelemetryDocumentFactory.createInstantDocument(point.getEpochNanos()));
        document.append("attributes", TelemetryDocumentFactory.toDocument(point.getAttributes()));
        document.append("resource", TelemetryDocumentFactory.createResourceDocument(metric.getResource()));
        document.append("instrumentationScope",
            TelemetryDocumentFactory.createInstrumentationScopeDocument(metric.getInstrumentationScopeInfo()));
        List<Document> exemplars = extractExemplars(point.getExemplars());
        if (!exemplars.isEmpty()) {
            document.append("exemplars", exemplars);
        }
        return document;
    }

    private List<Document> extractExemplars(List<? extends ExemplarData> exemplars) {
        if (exemplars == null || exemplars.isEmpty()) {
            return List.of();
        }
        List<Document> documents = new ArrayList<>(exemplars.size());
        for (ExemplarData exemplar : exemplars) {
            Document document = new Document()
                .append("time", TelemetryDocumentFactory.createInstantDocument(exemplar.getEpochNanos()))
                .append("filteredAttributes", TelemetryDocumentFactory.toDocument(exemplar.getFilteredAttributes()))
                .append("spanContext", new Document()
                    .append("traceId", exemplar.getSpanContext().getTraceId())
                    .append("spanId", exemplar.getSpanContext().getSpanId()));
            if (exemplar instanceof DoubleExemplarData doubleExemplar) {
                document.append("value", doubleExemplar.getValue());
            } else if (exemplar instanceof LongExemplarData longExemplar) {
                document.append("value", longExemplar.getValue());
            }
            documents.add(document);
        }
        return documents;
    }
}
