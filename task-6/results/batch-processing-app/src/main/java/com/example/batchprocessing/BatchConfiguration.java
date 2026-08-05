package com.example.batchprocessing;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class BatchConfiguration {

	@Bean
	public FlatFileItemReader<Product> reader() {
		return new FlatFileItemReaderBuilder<Product>()
			.name("productItemReader")
			// файл лежит в src/main/resources и попадает внутрь jar,
			// поэтому читаем его как classpath-ресурс, а не с диска
			.resource(new ClassPathResource("product-data.csv"))
			.delimited()
			// порядок имён должен совпадать с порядком колонок в product-data.csv
			.names("productId", "productSku", "productName", "productAmount", "productData")
			.targetType(Product.class)
			.build();
	}

	@Bean
	public ProductItemProcessor processor() {
		return new ProductItemProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<Product> writer(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Product>()
			// upsert, а не чистый INSERT: в отличие от заданий 4-5, эндпойнт в задании 6
			// можно вызывать многократно — без ON CONFLICT второй вызов падал бы
			// с DuplicateKeyException на том же productId
			.sql("INSERT INTO products (productId, productSku, productName, productAmount, productData) "
				+ "VALUES (:productId, :productSku, :productName, :productAmount, :productData) "
				+ "ON CONFLICT (productId) DO UPDATE SET "
				+ "productSku = EXCLUDED.productSku, "
				+ "productName = EXCLUDED.productName, "
				+ "productAmount = EXCLUDED.productAmount, "
				+ "productData = EXCLUDED.productData")
			.dataSource(dataSource)
			// значения параметров берутся из аксессоров Product (productId(), productSku()...)
			// по имени параметра — соответствие настраивать вручную не нужно
			.beanMapped()
			.build();
	}

	@Bean
	public Job importProductJob(JobRepository jobRepository, Step step1, JobCompletionNotificationListener listener) {
		return new JobBuilder("importProductJob", jobRepository)
			// после завершения Job сработает наш листенер и залогирует результат
			.listener(listener)
			.start(step1)
			.build();
	}

	@Bean
	public Step step1(JobRepository jobRepository, DataSourceTransactionManager transactionManager,
					  FlatFileItemReader<Product> reader, ProductItemProcessor processor, JdbcBatchItemWriter<Product> writer) {
		return new StepBuilder("step1", jobRepository)
			// chunk-обработка: копим по 3 обработанные строки и пишем их в БД одним
			// пакетом в одной транзакции — при сбое откатится только текущий чанк,
			// а не уже записанные ранее строки
			.<Product, Product>chunk(3, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.build();
	}

}
