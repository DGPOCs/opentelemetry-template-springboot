# Weather Service con OpenTelemetry

Servicio backend en Spring Boot que expone un endpoint para consultar el clima utilizando la API One Call de [OpenWeatherMap](https://openweathermap.org/api/one-call-3) e integra trazas y métricas con OpenTelemetry.

## Requisitos

- Java 17
- Maven 3.9+
- Una cuenta de OpenWeatherMap con una API key válida

## Configuración

El servicio utiliza las siguientes propiedades (también disponibles vía variables de entorno):

| Propiedad | Variable de entorno | Descripción |
|-----------|---------------------|-------------|
| `weather.api-key` | `OPENWEATHER_API_KEY` | API key de OpenWeatherMap (obligatoria). |
| `weather.base-url` | — | URL base del endpoint One Call. Por defecto `https://api.openweathermap.org/data/3.0`. |
| `weather.units` | — | Sistema de unidades (`standard`, `metric`, `imperial`). Por defecto `metric`. |
| `weather.language` | — | Idioma de las respuestas. Por defecto `es`. |
| `management.otlp.tracing.endpoint` | `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | Endpoint OTLP para exportar trazas. |
| `management.otlp.metrics.export.endpoint` | `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | Endpoint OTLP para exportar métricas. |

## Ejecución

1. Exporta la API key de OpenWeatherMap:
   ```bash
   export OPENWEATHER_API_KEY="tu_api_key"
   ```
2. (Opcional) Configura los endpoints de OpenTelemetry para trazas y métricas:
   ```bash
   export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT="http://localhost:4318/v1/traces"
   export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT="http://localhost:4318/v1/metrics"
   ```
3. Compila y ejecuta la aplicación:
   ```bash
   mvn spring-boot:run
   ```

## Endpoint disponible

```
GET /api/weather?lat={latitud}&lon={longitud}&units={unidades?}&lang={idioma?}
```

- `lat` y `lon` son obligatorios.
- `units` y `lang` son opcionales; si no se envían se toman de la configuración.

La respuesta incluye:

- Clima actual (`current`)
- Pronóstico minucioso para la próxima hora (`minutely`)
- Pronóstico por hora para 48 horas (`hourly`)
- Pronóstico diario para 8 días (`daily`)
- Alertas emitidas por autoridades (`alerts`)

## Observabilidad

El proyecto incorpora:

- `micrometer-tracing-bridge-otel` para publicar trazas con OpenTelemetry.
- `micrometer-registry-otlp` para exportar métricas vía OTLP.
- Instrumentación de clientes HTTP mediante `ObservationRestTemplateCustomizer` y el ecosistema de Spring Observability.

Los endpoints de Actuator expuestos incluyen `health`, `info` y `prometheus`. Ajusta la configuración de `management` según tus necesidades.
