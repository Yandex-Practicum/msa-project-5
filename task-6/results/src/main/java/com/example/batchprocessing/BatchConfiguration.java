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
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class BatchConfiguration {

	/** Размер чанка: записи читаются, обогащаются и вставляются в БД пакетами по 100 строк в одной транзакции. */
	private static final int CHUNK_SIZE = 100;

	@Bean
	public FlatFileItemReader<Product> reader() {
		return new FlatFileItemReaderBuilder<Product>()
			.name("productItemReader")
			.resource(new ClassPathResource("product-data.csv"))
			.delimited()
			.names("productId", "productSku", "productName", "productAmount", "productData")
			.targetType(Product.class)
			.build();
	}

	@Bean
	public ProductItemProcessor processor(JdbcTemplate jdbcTemplate) {
		return new ProductItemProcessor(jdbcTemplate);
	}

	@Bean
	public JdbcBatchItemWriter<Product> writer(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Product>()
			.sql("INSERT INTO products (productId, productSku, productName, productAmount, productData) " +
					"VALUES (:productId, :productSku, :productName, :productAmount, :productData)")
			.dataSource(dataSource)
			.beanMapped()
			.build();
	}

	@Bean
	public Job importProductJob(JobRepository jobRepository, Step importProductsStep,
								JobCompletionNotificationListener listener) {
		return new JobBuilder("importProductJob", jobRepository)
			.listener(listener)
			.start(importProductsStep)
			.build();
	}

	@Bean
	public Step importProductsStep(JobRepository jobRepository, DataSourceTransactionManager transactionManager,
								   FlatFileItemReader<Product> reader, ProductItemProcessor processor,
								   JdbcBatchItemWriter<Product> writer) {
		return new StepBuilder("importProductsStep", jobRepository)
			.<Product, Product>chunk(CHUNK_SIZE, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			// Отчёт не должен падать целиком из-за отдельных битых строк:
			// пропускаем до 10 некорректных записей с записью в лог.
			.faultTolerant()
			.skip(FlatFileParseException.class)
			.skipLimit(10)
			.build();
	}

}
