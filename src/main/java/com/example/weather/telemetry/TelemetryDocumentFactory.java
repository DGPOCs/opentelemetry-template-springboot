package com.example.weather.telemetry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bson.Document;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;

final class TelemetryDocumentFactory {

    private TelemetryDocumentFactory() {
    }

    static Document createResourceDocument(Resource resource) {
        Document resourceDoc = new Document();
        resourceDoc.append("schemaUrl", resource.getSchemaUrl());
        resourceDoc.append("attributes", toDocument(resource.getAttributes()));
        return resourceDoc;
    }

    static Document createInstrumentationScopeDocument(InstrumentationScopeInfo info) {
        Document scope = new Document();
        scope.append("name", info.getName());
        scope.append("version", info.getVersion());
        scope.append("schemaUrl", info.getSchemaUrl());
        if (!info.getAttributes().isEmpty()) {
            scope.append("attributes", toDocument(info.getAttributes()));
        }
        return scope;
    }

    static Document createInstantDocument(long epochNanos) {
        long seconds = epochNanos / 1_000_000_000L;
        long nanos = epochNanos % 1_000_000_000L;
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        return new Document(Map.of(
            "epochSeconds", seconds,
            "epochNanos", epochNanos,
            "iso", instant.toString()));
    }

    static Document toDocument(Attributes attributes) {
        Document document = new Document();
        attributes.forEach((key, value) -> document.append(key.getKey(), sanitizeValue(value)));
        return document;
    }

    static Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> sanitizeValue(e.getValue())));
        }
        if (value instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                .map(TelemetryDocumentFactory::sanitizeValue)
                .collect(Collectors.toList());
        }
        if (value.getClass().isArray()) {
            return sanitizeArray(value);
        }
        return value;
    }

    private static Object sanitizeArray(Object array) {
        if (array instanceof Object[] objects) {
            return Arrays.stream(objects)
                .map(TelemetryDocumentFactory::sanitizeValue)
                .collect(Collectors.toList());
        }
        if (array instanceof long[] longs) {
            return Arrays.stream(longs).boxed().collect(Collectors.toList());
        }
        if (array instanceof int[] ints) {
            return Arrays.stream(ints).boxed().collect(Collectors.toList());
        }
        if (array instanceof double[] doubles) {
            return Arrays.stream(doubles).boxed().collect(Collectors.toList());
        }
        if (array instanceof float[] floats) {
            List<Double> values = new ArrayList<>(floats.length);
            for (float element : floats) {
                values.add((double) element);
            }
            return values;
        }
        if (array instanceof short[] shorts) {
            List<Integer> values = new ArrayList<>(shorts.length);
            for (short element : shorts) {
                values.add((int) element);
            }
            return values;
        }
        if (array instanceof byte[] bytes) {
            List<Integer> values = new ArrayList<>(bytes.length);
            for (byte element : bytes) {
                values.add((int) element);
            }
            return values;
        }
        if (array instanceof boolean[] booleans) {
            List<Boolean> values = new ArrayList<>(booleans.length);
            for (boolean element : booleans) {
                values.add(element);
            }
            return values;
        }
        if (array instanceof char[] chars) {
            return new String(chars);
        }
        return Stream.of(array)
            .map(TelemetryDocumentFactory::sanitizeValue)
            .collect(Collectors.toList());
    }
}
