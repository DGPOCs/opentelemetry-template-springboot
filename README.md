# Weather Service con OpenTelemetry

Servicio backend en Spring Boot que expone un endpoint para consultar el clima utilizando la API One Call de [OpenWeatherMap](https://openweathermap.org/api/one-call-3) e integra trazas y métricas con OpenTelemetry.

## Requisitos

- Java 17
- Maven 3.9+
- Una cuenta de OpenWeatherMap con una API key válida

## Configuración

Todas las variables de entorno utilizadas por la aplicación y la infraestructura auxiliar se definen en el archivo `.env` de la raíz del proyecto. Puedes modificar los valores por defecto o crear una copia local del archivo si necesitas credenciales específicas.

El servicio utiliza las siguientes propiedades (también disponibles vía variables de entorno):

| Propiedad | Variable de entorno | Descripción |
|-----------|---------------------|-------------|
| `weather.api-key` | `OPENWEATHER_API_KEY` | API key de OpenWeatherMap (obligatoria). |
| `weather.base-url` | — | URL base del endpoint One Call. Por defecto `https://api.openweathermap.org/data/3.0`. |
| `weather.units` | — | Sistema de unidades (`standard`, `metric`, `imperial`). Por defecto `metric`. |
| `weather.language` | — | Idioma de las respuestas. Por defecto `es`. |
| `management.otlp.tracing.endpoint` | `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | Endpoint OTLP para exportar trazas. |
| `management.otlp.metrics.export.endpoint` | `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | Endpoint OTLP para exportar métricas. |
| `telemetry.mongo.uri` | `MONGODB_URI` | URI de conexión a MongoDB donde se guardan logs, trazas y métricas. |
| `telemetry.mongo.database` | `MONGODB_DATABASE` | Base de datos objetivo para la telemetría. |
| `telemetry.mongo.logs-collection` | `MONGODB_COLLECTION_LOGS` | Colección usada para almacenar logs. |
| `telemetry.mongo.traces-collection` | `MONGODB_COLLECTION_TRACES` | Colección usada para almacenar trazas. |
| `telemetry.mongo.metrics-collection` | `MONGODB_COLLECTION_METRICS` | Colección usada para almacenar métricas. |
| `telemetry.mongo.metrics-export-interval` | `MONGODB_METRICS_EXPORT_INTERVAL` | Intervalo ISO-8601 para persistir métricas (por defecto `PT30S`). |
| — | `OTEL_LOGS_EXPORTER` | Define si se exportan logs vía OTLP (`otlp`) además de MongoDB. |

## Ejecución

1. Exporta la API key de OpenWeatherMap:
   ```bash
   export OPENWEATHER_API_KEY="tu_api_key"
   ```
2. Configura las variables de conexión a MongoDB (si la instancia no usa los valores por defecto):
   ```bash
   export MONGODB_URI="mongodb://usuario:password@localhost:27017"
   export MONGODB_DATABASE="openweather_telemetry"
   export MONGODB_COLLECTION_LOGS="logs"
   export MONGODB_COLLECTION_TRACES="traces"
   export MONGODB_COLLECTION_METRICS="metrics"
   ```
3. (Opcional) Configura los endpoints de OpenTelemetry para trazas y métricas:
   ```bash
   export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT="http://localhost:4318/v1/traces"
   export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT="http://localhost:4318/v1/metrics"
   ```
4. Compila y ejecuta la aplicación:
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

- Exportadores personalizados que almacenan logs, trazas y métricas directamente en MongoDB.
- `micrometer-tracing-bridge-otel` para publicar trazas con OpenTelemetry.
- `micrometer-registry-otlp` para exportar métricas vía OTLP.
- Instrumentación de clientes HTTP mediante `ObservationRestTemplateCustomizer` y el ecosistema de Spring Observability.

Los endpoints de Actuator expuestos incluyen `health`, `info` y `prometheus`. Ajusta la configuración de `management` según tus necesidades.

## Desarrollo en Dev Container

1. Instala la extensión **Dev Containers** en VS Code.
2. Abre la carpeta del proyecto y selecciona `Dev Containers: Reopen in Container`.
3. Las variables definidas en `.env` se cargan automáticamente dentro del contenedor de desarrollo, por lo que puedes ajustar la configuración del servicio editando ese archivo antes de reconstruirlo.
4. El entorno incluye Java 17, Maven y Docker CLI listo para construir y ejecutar contenedores.

## Despliegue con Docker

Para construir la imagen y ejecutar todos los servicios (aplicación, MongoDB y colector OTEL de referencia):

```bash
docker compose up --build
```

Las variables de entorno se cargan desde el archivo `.env`. Puedes modificarlo o exportar variables adicionales en la línea de comandos antes de ejecutar `docker compose` para sobrescribir los valores. Por defecto:

- La aplicación queda disponible en `http://localhost:8080`.
- MongoDB se expone en `mongodb://localhost:27017`.
- El colector OTLP escucha en `http://localhost:4318`.
