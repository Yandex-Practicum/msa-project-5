FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Maven кладёт jar в target/
COPY target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
