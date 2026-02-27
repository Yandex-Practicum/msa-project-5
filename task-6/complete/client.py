import time
import requests
import sys

print("Скрипт запущен...")  # Отладочный вывод

ENDPOINT = "http://localhost:8080/api/batch/run"

def main():
    print(f"Подключение к {ENDPOINT}...")
    
    try:
        start_time = time.time()
        response = requests.post(ENDPOINT, timeout=10)
        elapsed_time = time.time() - start_time
        
        print(f"Статус ответа: {response.status_code}")
        print(f"Время выполнения: {elapsed_time:.3f} сек")
        print()
        print("Ответ от сервера:")
        
        data = response.json()
        for key, value in data.items():
            print(f"{key:10}: {value}")
        
        print()
        print(f"Трейс в Jaeger UI: http://localhost:16686/trace/{data.get('traceId')}")
        print()
        
        if data.get("status") == "success":
            print("Batch job успешно запущен!")
        else:
            print(f"Ошибка при запуске batch job: {data.get('message')}")
            
    except requests.exceptions.ConnectionError:
        print("ОШИБКА: Не удалось подключиться к серверу. Убедитесь, что Spring Boot приложение запущено на порту 8080")
        sys.exit(1)
    except Exception as e:
        print(f"ОШИБКА: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()