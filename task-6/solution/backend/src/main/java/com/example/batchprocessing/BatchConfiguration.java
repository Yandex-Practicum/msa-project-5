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
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class BatchConfiguration {
    @Bean
    FlatFileItemReader<Product> reader(@Value("${app.input:classpath:product-data.csv}") Resource input) {
        return new FlatFileItemReaderBuilder<Product>()
            .name("productItemReader")
            .resource(input)
            .delimited().names("productId", "productSku", "productName", "productAmount", "productData")
            .targetType(Product.class).build();
    }

    @Bean
    JdbcBatchItemWriter<Product> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Product>()
            .dataSource(dataSource).beanMapped()
            .sql("""
                INSERT INTO products (productId, productSku, productName, productAmount, productData)
                VALUES (:productId, :productSku, :productName, :productAmount,
                    COALESCE((SELECT loyalityData FROM loyality_data WHERE productSku = :productSku), :productData))
                ON CONFLICT (productId) DO UPDATE SET productSku = EXCLUDED.productSku,
                    productName = EXCLUDED.productName, productAmount = EXCLUDED.productAmount,
                    productData = EXCLUDED.productData
                """)
            .build();
    }

    @Bean
    Job importProductJob(JobRepository repository, Step importStep, JobCompletionNotificationListener listener) {
        return new JobBuilder("importProductJob", repository).listener(listener).start(importStep).build();
    }

    @Bean
    Step importStep(JobRepository repository, DataSourceTransactionManager transactionManager,
                    FlatFileItemReader<Product> reader, ProductItemProcessor processor, JdbcBatchItemWriter<Product> writer) {
        return new StepBuilder("importStep", repository).<Product, Product>chunk(100, transactionManager)
            .reader(reader).processor(processor).writer(writer).build();
    }
}
