FROM gradle:8.8-jdk17 AS build
WORKDIR /workspace

COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle
COPY src ./src

RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
