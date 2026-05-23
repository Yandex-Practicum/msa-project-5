# Stage 1: Build using global gradle command
FROM gradle:8-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
# Changed from ./gradlew to gradle
RUN gradle bootJar --no-daemon 

# Stage 2: Runtime
FROM openjdk:27-ea-slim
EXPOSE 8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]