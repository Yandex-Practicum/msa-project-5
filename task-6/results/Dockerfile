FROM gradle:8.8-jdk17-alpine AS build

WORKDIR /workspace
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle src src
RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S batch && adduser -S batch -G batch
WORKDIR /app
COPY --from=build --chown=batch:batch /workspace/build/libs/batch-processing.jar app.jar

USER batch
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
