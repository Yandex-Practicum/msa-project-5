FROM gradle:8.8-jdk17 AS build
WORKDIR /workspace

COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle
COPY src ./src

RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:17-jre
ARG JMX_EXPORTER_VERSION=1.2.0
WORKDIR /app

RUN mkdir -p /app/logs /opt/jmx-exporter

ADD https://github.com/prometheus/jmx_exporter/releases/download/${JMX_EXPORTER_VERSION}/jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar /opt/jmx-exporter/jmx_prometheus_javaagent.jar

COPY jmx-exporter/config.yml /opt/jmx-exporter/config.yml
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-javaagent:/opt/jmx-exporter/jmx_prometheus_javaagent.jar=9404:/opt/jmx-exporter/config.yml", "-jar", "/app/app.jar"]
