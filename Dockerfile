# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
ENV APP_HOME=/app
WORKDIR ${APP_HOME}
COPY --from=build /workspace/target/weather-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
