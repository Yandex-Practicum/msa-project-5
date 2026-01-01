FROM eclipse-temurin:17-jre-jammy  # у меня не заводится на предложенной openjdk:24-slim

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
