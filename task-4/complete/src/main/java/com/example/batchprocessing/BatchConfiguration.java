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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class BatchConfiguration {
	@Bean
    public FlatFileItemReader<Loyality> loyalityReader() {
        return new FlatFileItemReaderBuilder<Loyality>()
            .name("loyalityItemReader")
            .resource(new ClassPathResource("loyality_data.csv"))
            .delimited()
            .names("productSku", "loyalityData")
            .targetType(Loyality.class)
            .build();
    }

    @Bean
    public JdbcBatchItemWriter<Loyality> loyalityWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Loyality>()
            .sql("INSERT INTO loyality_data (productSku, loyalityData) " +
                    "VALUES (:productSku, :loyalityData)")
            .dataSource(dataSource)
            .beanMapped()
            .build();
    }

    @Bean
    public Step step0(JobRepository jobRepository, DataSourceTransactionManager transactionManager,
            FlatFileItemReader<Loyality> loyalityReader, JdbcBatchItemWriter<Loyality> loyalityWriter) {
        return new StepBuilder("step0", jobRepository)
                .<Loyality, Loyality>chunk(10, transactionManager)
                .reader(loyalityReader)
                .writer(loyalityWriter)
                .build();
    }
	
	@Bean
	public FlatFileItemReader<Product> reader() {
		return new FlatFileItemReaderBuilder<Product>()
			.name("productItemReader")
			.resource(new ClassPathResource("product-data.csv"))
			.delimited()
			.names("productId", "productSku","productName", "productAmount", "productData")
			.targetType(Product.class)
			.build();
	}

	@Bean
	public ProductItemProcessor processor() {
		return new ProductItemProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<Product> productWriter(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Product>()
			.sql("INSERT INTO products (productId, productSku, productName, productAmount, productData) " +
					"VALUES (:productId, :productSku, :productName, :productAmount, :productData)")
			.dataSource(dataSource)
			.beanMapped()
			.build();
	}

	@Bean
	public Job importProductJob(JobRepository jobRepository, Step step0, Step step1, JobCompletionNotificationListener listener) {
		return new JobBuilder("importProductJob", jobRepository)
			.listener(listener)
            .start(step0) // loyality
			.next(step1)  // product
			.build();
	}

	@Bean
	public Step step1(JobRepository jobRepository, DataSourceTransactionManager transactionManager,
					  FlatFileItemReader<Product> reader, ProductItemProcessor processor, JdbcBatchItemWriter<Product> productWriter) {
		return new StepBuilder("step1", jobRepository)
			.<Product, Product>chunk(3, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(productWriter)
			.build();
	}

}
