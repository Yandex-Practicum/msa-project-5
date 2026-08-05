FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY app/pom.xml pom.xml
COPY app/src src
RUN mvn --batch-mode package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S exporter && adduser -S exporter -G exporter
WORKDIR /app
COPY --from=build --chown=exporter:exporter /build/target/shipment-exporter.jar app.jar

USER exporter
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
