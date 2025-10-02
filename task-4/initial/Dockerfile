FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем gradle wrapper и build файлы
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Даем права на выполнение gradlew
RUN chmod +x ./gradlew

# Копируем исходный код
COPY src src

# Собираем приложение
RUN ./gradlew build -x test

# Копируем jar файл
RUN cp build/libs/batch-processing-0.0.1-SNAPSHOT.jar ./app.jar

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]