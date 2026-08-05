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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class BatchConfiguration {

    @Bean
    public FlatFileItemReader<Product> reader(
            @Value("${batch.input-resource}") Resource inputResource
    ) {
        return new FlatFileItemReaderBuilder<Product>()
                .name("productItemReader")
                .resource(inputResource)
                .strict(true)
                .delimited()
                .delimiter(",")
                .names("productId", "productSku", "productName", "productAmount", "productData")
                .fieldSetMapper(fields -> new Product(
                        fields.readLong("productId"),
                        fields.readLong("productSku"),
                        fields.readString("productName"),
                        fields.readLong("productAmount"),
                        fields.readString("productData")
                ))
                .build();
    }

    @Bean
    public ProductItemProcessor processor(JdbcTemplate jdbcTemplate) {
        return new ProductItemProcessor(jdbcTemplate);
    }

    @Bean
    public JdbcBatchItemWriter<Product> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Product>()
                .sql("""
                        INSERT INTO products (
                            product_id, product_sku, product_name, product_amount, product_data
                        ) VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (product_id) DO UPDATE SET
                            product_sku = EXCLUDED.product_sku,
                            product_name = EXCLUDED.product_name,
                            product_amount = EXCLUDED.product_amount,
                            product_data = EXCLUDED.product_data
                        """)
                .itemPreparedStatementSetter((product, statement) -> {
                    statement.setLong(1, product.productId());
                    statement.setLong(2, product.productSku());
                    statement.setString(3, product.productName());
                    statement.setLong(4, product.productAmount());
                    statement.setString(5, product.productData());
                })
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Job importProductJob(
            JobRepository jobRepository,
            Step importProductsStep,
            JobCompletionNotificationListener listener
    ) {
        return new JobBuilder("importProductJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(importProductsStep)
                .build();
    }

    @Bean
    public Step importProductsStep(
            JobRepository jobRepository,
            DataSourceTransactionManager transactionManager,
            FlatFileItemReader<Product> reader,
            ProductItemProcessor processor,
            JdbcBatchItemWriter<Product> writer,
            @Value("${batch.chunk-size}") int chunkSize
    ) {
        return new StepBuilder("importProductsStep", jobRepository)
                .<Product, Product>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .build();
    }
}
