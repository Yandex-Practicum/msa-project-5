FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build.gradle .
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY src src

RUN ./gradlew build -x test

EXPOSE 8081

CMD ["java", "-jar", "build/libs/batch-client-0.0.1-SNAPSHOT.jar"]
