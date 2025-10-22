package com.example.weather.telemetry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;

@Configuration
@EnableConfigurationProperties(MongoTelemetryProperties.class)
public class MongoTelemetryConfiguration {

    @Bean
    public MongoClient mongoClient(MongoTelemetryProperties properties) {
        return MongoClients.create(properties.getUri());
    }

    @Bean
    public MongoDatabase telemetryMongoDatabase(MongoClient client, MongoTelemetryProperties properties) {
        return client.getDatabase(properties.getDatabase());
    }

    @Bean
    public MongoCollection<Document> logsCollection(MongoDatabase database, MongoTelemetryProperties properties) {
        return database.getCollection(properties.getLogsCollection());
    }

    @Bean
    public MongoCollection<Document> tracesCollection(MongoDatabase database, MongoTelemetryProperties properties) {
        return database.getCollection(properties.getTracesCollection());
    }

    @Bean
    public MongoCollection<Document> metricsCollection(MongoDatabase database, MongoTelemetryProperties properties) {
        return database.getCollection(properties.getMetricsCollection());
    }

    @Bean
    public MongoLogRecordExporter mongoLogRecordExporter(MongoCollection<Document> logsCollection) {
        return new MongoLogRecordExporter(logsCollection);
    }

    @Bean
    public MongoSpanExporter mongoSpanExporter(MongoCollection<Document> tracesCollection) {
        return new MongoSpanExporter(tracesCollection);
    }

    @Bean
    public MongoMetricExporter mongoMetricExporter(MongoCollection<Document> metricsCollection) {
        return new MongoMetricExporter(metricsCollection);
    }

    @Bean
    public LogRecordExporter logRecordExporter(MongoLogRecordExporter mongoLogRecordExporter,
            ObjectProvider<OtlpGrpcLogRecordExporter> otlpProvider) {
        List<LogRecordExporter> exporters = new ArrayList<>();
        exporters.add(mongoLogRecordExporter);
        otlpProvider.ifAvailable(exporters::add);
        return new DelegatingLogRecordExporter(exporters);
    }

    @Bean
    public SpanExporter spanExporter(MongoSpanExporter mongoSpanExporter,
            ObjectProvider<OtlpGrpcSpanExporter> otlpProvider) {
        List<SpanExporter> exporters = new ArrayList<>();
        exporters.add(mongoSpanExporter);
        otlpProvider.ifAvailable(exporters::add);
        return new DelegatingSpanExporter(exporters);
    }

    @Bean
    public MetricExporter metricExporter(MongoMetricExporter mongoMetricExporter,
            ObjectProvider<OtlpGrpcMetricExporter> otlpProvider) {
        List<MetricExporter> exporters = new ArrayList<>();
        exporters.add(mongoMetricExporter);
        otlpProvider.ifAvailable(exporters::add);
        return new DelegatingMetricExporter(exporters);
    }

    @Bean
    @ConditionalOnProperty(prefix = "management.otlp.metrics.export", name = "enabled", havingValue = "true")
    public OtlpGrpcMetricExporter otlpGrpcMetricExporter() {
        return OtlpGrpcMetricExporter.builder().build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "management.otlp.tracing", name = "endpoint")
    public OtlpGrpcSpanExporter otlpGrpcSpanExporter() {
        return OtlpGrpcSpanExporter.builder().build();
    }

    @Bean
    @ConditionalOnProperty(name = "otel.logs.exporter", havingValue = "otlp")
    public OtlpGrpcLogRecordExporter otlpGrpcLogRecordExporter() {
        return OtlpGrpcLogRecordExporter.builder().build();
    }

    @Bean(destroyMethod = "close")
    @Primary
    public OpenTelemetrySdk openTelemetrySdk(Environment environment, SpanExporter spanExporter,
            MetricExporter metricExporter, LogRecordExporter logRecordExporter,
            MongoTelemetryProperties properties) {
        Resource resource = Resource.getDefault().merge(Resource.builder()
            .put(ResourceAttributes.SERVICE_NAME, environment.getProperty("spring.application.name", "weather-service"))
            .build());

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

        Duration exportInterval = properties.getMetricsExportInterval();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                .setInterval(exportInterval)
                .build())
            .build();

        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(logRecordExporter))
            .build();

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(meterProvider)
            .setLoggerProvider(loggerProvider)
            .buildAndRegisterGlobal();
        return openTelemetrySdk;
    }
}
