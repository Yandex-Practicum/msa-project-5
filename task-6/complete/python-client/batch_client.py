import requests
import logging
import time
import sys
from typing import Dict, Any

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class BatchJobClient:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.trigger_endpoint = f"{self.base_url}/api/batch/trigger"
        
    def trigger_job(self) -> Dict[str, Any]:
        logger.info(f"Triggering batch job at {self.trigger_endpoint}")

        try:
            response = requests.post(
                self.trigger_endpoint,
                timeout=300
            )
            
            response.raise_for_status()
            
            result = response.json()
            logger.info(f"Batch job triggered successfully. Job ID: {result.get('jobId')}")
            logger.info(f"Job Status: {result.get('status')}")
            logger.info(f"Exit Status: {result.get('exitStatus')}")
            
            return result
        except Exception as e:
            logger.exception(f"Failed to run a job: {e}")
            raise
            
    def is_api_healthy(self) -> bool:
        health_url = f"{self.base_url}/actuator/health"
        
        try:
            logger.info(f"Checking API health at {health_url}")
            response = requests.get(health_url, timeout=5)
            response.raise_for_status()
            
            health_data = response.json()
            status = health_data.get('status', 'UNKNOWN')
            
            logger.info(f"API health status: {status}")
            return status == 'UP'
            
        except Exception as e:
            logger.warning(f"Health check failed: {e}")
            return False


def main():
    client = BatchJobClient()
    
    logger.info("Start Batch Job Client")

    logger.info("Checking if API is available...")

    if not client.is_api_healthy():
        logger.error("👎 API is not available")
        sys.exit(1)

    logger.info("👍 API is ready!")

    try:
        result = client.trigger_job()
        
        logger.info("Job Execution Details")
        logger.info(f"Job ID: {result.get('jobId')}")
        logger.info(f"Status: {result.get('status')}")
        logger.info(f"Exit Status: {result.get('exitStatus')}")
        logger.info(f"Start Time: {result.get('startTime')}")
        logger.info(f"End Time: {result.get('endTime')}")
    except Exception as e:
        logger.exception(f"💥 Failed to execute a batch job: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()