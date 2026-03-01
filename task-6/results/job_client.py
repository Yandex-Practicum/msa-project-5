#!/usr/bin/env python3
import requests
import uuid
import json
import logging
import time
from datetime import datetime

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('batch-job-client')

class BatchJobClient:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
        self.session = requests.Session()
    
    def generate_trace_id(self):
        """Генерирует уникальный trace ID"""
        return uuid.uuid4().hex
    
    def check_health(self):
        """Проверяет доступность приложения"""
        try:
            response = self.session.get(f"{self.base_url}/api/v1/jobs/status")
            if response.status_code == 200:
                logger.info("Application is healthy")
                return True
            else:
                logger.error(f"Application returned status {response.status_code}")
                return False
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to connect to application: {e}")
            return False
    
    def run_job(self, trace_id=None):
        """Запускает batch job"""
        if trace_id is None:
            trace_id = self.generate_trace_id()
        
        headers = {
            'X-Trace-Id': trace_id,
            'Content-Type': 'application/json'
        }
        
        logger.info(f"=== JOB CLIENT STARTED ===")
        logger.info(f"Trace ID: {trace_id}")
        logger.info(f"URL: {self.base_url}/api/v1/jobs/run")
        logger.info(f"Timestamp: {datetime.now().isoformat()}")
        
        start_time = time.time()
        
        try:
            response = self.session.post(
                f"{self.base_url}/api/v1/jobs/run",
                headers=headers,
                timeout=60
            )
            
            duration = (time.time() - start_time) * 1000
            
            logger.info(f"=== JOB CLIENT COMPLETED ===")
            logger.info(f"Status Code: {response.status_code}")
            logger.info(f"Duration: {duration:.2f} ms")
            logger.info(f"Trace ID: {trace_id}")
            
            if response.status_code == 200:
                result = response.json()
                logger.info(f"Job Result: {json.dumps(result, indent=2)}")
                return result
            else:
                logger.error(f"Error response: {response.text}")
                return None
                
        except requests.exceptions.Timeout:
            logger.error("Request timeout - job may still be running")
            logger.info(f"Trace ID: {trace_id}")
            return None
        except requests.exceptions.RequestException as e:
            logger.error(f"Request failed: {e}")
            logger.info(f"Trace ID: {trace_id}")
            return None

def main():
    client = BatchJobClient()
    
    # Проверяем здоровье приложения
    if not client.check_health():
        logger.error("Application is not available. Exiting.")
        return
    
    # Запускаем несколько job с разными trace ID
    for i in range(3):
        logger.info(f"\n=== RUNNING JOB #{i+1} ===")
        trace_id = client.generate_trace_id()
        result = client.run_job(trace_id)
        
        if result:
            logger.info(f"Job #{i+1} completed with status: {result.get('status')}")
        
        # Пауза между запусками
        if i < 2:
            time.sleep(2)
    
    # Демонстрация трейсинга - запуск с переданным trace ID
    logger.info("\n=== RUNNING JOB WITH SPECIFIC TRACE ID ===")
    specific_trace = "test-trace-123456789"
    client.run_job(specific_trace)

if __name__ == "__main__":
    main()