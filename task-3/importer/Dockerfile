FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/importer-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]