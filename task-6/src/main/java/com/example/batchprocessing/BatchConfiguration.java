package com.example.batchprocessing;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class BatchConfiguration {

	@Value("${app.input.loyalty-file:data/loyality_data.csv}")
	private String loyaltyFile;

	@Value("${app.input.product-file:data/product-data.csv}")
	private String productFile;

	@Bean
	public FlatFileItemReader<Loyality> loyaltyReader() {
		return new FlatFileItemReaderBuilder<Loyality>()
			.name("loyaltyItemReader")
			.resource(new FileSystemResource(loyaltyFile))
			.delimited()
			.names("productSku", "loyalityData")
			.targetType(Loyality.class)
			.build();
	}

	@Bean
	public FlatFileItemReader<Product> productReader() {
		return new FlatFileItemReaderBuilder<Product>()
			.name("productItemReader")
			.resource(new FileSystemResource(productFile))
			.delimited()
			.names("productId", "productSku", "productName", "productAmount", "productData")
			.targetType(Product.class)
			.build();
	}

	@Bean
	public ProductItemProcessor processor(LoyaltyDataLookup loyaltyDataLookup) {
		return new ProductItemProcessor(loyaltyDataLookup);
	}

	@Bean
	public JdbcBatchItemWriter<Loyality> loyaltyWriter(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Loyality>()
			.sql("""
				INSERT INTO loyality_data (productSku, loyalityData)
				VALUES (:productSku, :loyalityData)
				ON CONFLICT (productSku) DO UPDATE
				SET loyalityData = EXCLUDED.loyalityData
				""")
			.dataSource(dataSource)
			.beanMapped()
			.build();
	}

	@Bean
	public JdbcBatchItemWriter<Product> productWriter(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Product>()
			.sql("""
				INSERT INTO products (productId, productSku, productName, productAmount, productData)
				VALUES (:productId, :productSku, :productName, :productAmount, :productData)
				ON CONFLICT (productId) DO UPDATE
				SET productSku = EXCLUDED.productSku,
				    productName = EXCLUDED.productName,
				    productAmount = EXCLUDED.productAmount,
				    productData = EXCLUDED.productData
				""")
			.dataSource(dataSource)
			.beanMapped()
			.build();
	}

	@Bean
	public Job importProductJob(
		JobRepository jobRepository,
		Step loadLoyaltyStep,
		Step importProductsStep,
		JobCompletionNotificationListener listener
	) {
		return new JobBuilder("importProductJob", jobRepository)
			.incrementer(new RunIdIncrementer())
			.listener(listener)
			.start(loadLoyaltyStep)
			.next(importProductsStep)
			.build();
	}

	@Bean
	public Step loadLoyaltyStep(
		JobRepository jobRepository,
		DataSourceTransactionManager transactionManager,
		FlatFileItemReader<Loyality> loyaltyReader,
		JdbcBatchItemWriter<Loyality> loyaltyWriter,
		StepExecutionLoggingListener stepExecutionLoggingListener
	) {
		return new StepBuilder("loadLoyaltyStep", jobRepository)
			.<Loyality, Loyality>chunk(10, transactionManager)
			.reader(loyaltyReader)
			.writer(loyaltyWriter)
			.listener(stepExecutionLoggingListener)
			.build();
	}

	@Bean
	public Step importProductsStep(
		JobRepository jobRepository,
		DataSourceTransactionManager transactionManager,
		FlatFileItemReader<Product> productReader,
		ProductItemProcessor processor,
		JdbcBatchItemWriter<Product> productWriter,
		StepExecutionLoggingListener stepExecutionLoggingListener
	) {
		return new StepBuilder("importProductsStep", jobRepository)
			.<Product, Product>chunk(10, transactionManager)
			.reader(productReader)
			.processor(processor)
			.writer(productWriter)
			.listener(stepExecutionLoggingListener)
			.build();
	}
}
