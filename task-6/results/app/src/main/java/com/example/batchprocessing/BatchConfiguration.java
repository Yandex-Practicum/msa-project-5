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
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfiguration {
    @Bean
    public FlatFileItemReader<Product> reader() {
        return new FlatFileItemReaderBuilder<Product>()
            .name("productReader")
            .resource(new ClassPathResource("product-data.csv"))
            .delimited()
            .names("productId", "productSku", "productName", "productAmount", "productData")
            .targetType(Product.class)
            .build();
    }

    @Bean
    public JdbcBatchItemWriter<Product> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Product>()
            .sql("INSERT INTO products (product_id, product_sku, product_name, product_amount, product_data) VALUES (:productId, :productSku, :productName, :productAmount, :productData) ON CONFLICT (product_id) DO UPDATE SET product_sku = EXCLUDED.product_sku, product_name = EXCLUDED.product_name, product_amount = EXCLUDED.product_amount, product_data = EXCLUDED.product_data")
            .dataSource(dataSource)
            .beanMapped()
            .build();
    }

    @Bean
    public Step importStep(JobRepository repository, PlatformTransactionManager transactionManager, FlatFileItemReader<Product> reader, ProductItemProcessor processor, JdbcBatchItemWriter<Product> writer) {
        return new StepBuilder("importStep", repository)
            .<Product, Product>chunk(3, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    public Job importProductJob(JobRepository repository, Step importStep, JobCompletionNotificationListener listener) {
        return new JobBuilder("importProductJob", repository)
            .listener(listener)
            .start(importStep)
            .build();
    }
}
