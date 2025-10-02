#!/bin/bash

# Скрипт для синхронизации CSV файлов из minikube на хост

echo "Синхронизация CSV файлов из minikube..."

# Получаем список файлов в minikube
FILES=$(minikube ssh "ls /Users/bkhafizull/IdeaProjects/msa-project-5/task-3/results/csv/*.csv 2>/dev/null" | tr -d '\r')

if [ -z "$FILES" ]; then
    echo "Нет CSV файлов для синхронизации"
    exit 0
fi

# Копируем каждый файл отдельно
for file in $FILES; do
    filename=$(basename "$file")
    echo "Копирование $filename..."
    minikube cp "minikube:$file" "/Users/bkhafizull/IdeaProjects/msa-project-5/task-3/results/csv/$filename"
done

echo "Синхронизация завершена!"
echo "Файлы в папке:"
ls -la /Users/bkhafizull/IdeaProjects/msa-project-5/task-3/results/csv/*.csv 2>/dev/null || echo "Нет CSV файлов"
