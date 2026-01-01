FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY build/libs/*.jar app.jar
COPY prometheus/jmx_prometheus_javaagent.jar prometheus/
COPY prometheus/jmx_config.yml prometheus/

ENTRYPOINT ["java","-javaagent:/app/prometheus/jmx_prometheus_javaagent.jar=5555:/app/prometheus/jmx_config.yml","-jar","app.jar"]
