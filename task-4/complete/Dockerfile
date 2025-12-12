FROM eclipse-temurin:21-jre
# 8080
WORKDIR /app
COPY build/libs/app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]