package com.example.batchprocessing;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
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

    private final MeterRegistry meterRegistry;

    public BatchConfiguration(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public FlatFileItemReader<Product> reader() {
        return new FlatFileItemReaderBuilder<Product>()
                .name("productItemReader")
                .resource(new ClassPathResource("product-data.csv"))
                .delimited()
                .delimiter(",")
                .names("productId", "productSku", "productName", "productAmount", "productData")
                .linesToSkip(1)
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
                .itemSqlParameterSourceProvider(
                        new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO products (productId, productSku, productName, productAmount, productData) " +
                        "VALUES (:productId, :productSku, :productName, :productAmount, :productData) " +
                        "ON CONFLICT (productId) DO UPDATE SET " +
                        "productSku = EXCLUDED.productSku, " +
                        "productName = EXCLUDED.productName, " +
                        "productAmount = EXCLUDED.productAmount, " +
                        "productData = EXCLUDED.productData")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Job importProductJob(JobRepository jobRepository, Step step1,
                                JobCompletionNotificationListener listener) {
        return new JobBuilder("importProductJob", jobRepository)
                .listener(listener)
                .start(step1)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, DataSourceTransactionManager transactionManager,
                      FlatFileItemReader<Product> reader, ProductItemProcessor processor,
                      JdbcBatchItemWriter<Product> writer) {
        return new StepBuilder("step1", jobRepository)
                .<Product, Product>chunk(3, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(new StepMetricsListener(meterRegistry))
                .build();
    }
}