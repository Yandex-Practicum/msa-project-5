FROM eclipse-temurin:21-jre-noble
# 8080
WORKDIR .
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]