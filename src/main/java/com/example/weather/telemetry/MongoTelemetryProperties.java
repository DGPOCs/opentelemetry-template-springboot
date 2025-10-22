package com.example.weather.telemetry;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemetry.mongo")
public class MongoTelemetryProperties {

    /** MongoDB connection string. */
    private String uri = "mongodb://localhost:27017";

    /** Database used to store telemetry documents. */
    private String database = "telemetry";

    /** Collection used for application logs. */
    private String logsCollection = "logs";

    /** Collection used for trace spans. */
    private String tracesCollection = "traces";

    /** Collection used for metric samples. */
    private String metricsCollection = "metrics";

    /** Interval used to persist metrics from the OpenTelemetry meter provider. */
    private Duration metricsExportInterval = Duration.ofSeconds(30);

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getLogsCollection() {
        return logsCollection;
    }

    public void setLogsCollection(String logsCollection) {
        this.logsCollection = logsCollection;
    }

    public String getTracesCollection() {
        return tracesCollection;
    }

    public void setTracesCollection(String tracesCollection) {
        this.tracesCollection = tracesCollection;
    }

    public String getMetricsCollection() {
        return metricsCollection;
    }

    public void setMetricsCollection(String metricsCollection) {
        this.metricsCollection = metricsCollection;
    }

    public Duration getMetricsExportInterval() {
        return metricsExportInterval;
    }

    public void setMetricsExportInterval(Duration metricsExportInterval) {
        this.metricsExportInterval = metricsExportInterval;
    }
}
