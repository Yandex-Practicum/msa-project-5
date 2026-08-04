package com.example.batchprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BatchProcessingApplication {

	public static void main(String[] args) {
		// Без System.exit: после выполнения джобы приложение остаётся запущенным,
		// чтобы Prometheus мог собирать метрики с /actuator/prometheus.
		SpringApplication.run(BatchProcessingApplication.class, args);
	}

}
