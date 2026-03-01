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
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class BatchConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(BatchConfiguration.class);

    @Bean
    public FlatFileItemReader<Product> reader() {
        FlatFileItemReader<Product> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("product-data.csv"));
        reader.setLinesToSkip(1);
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("product_id", "product_sku", "product_name", "product_amount", "product_data");
        tokenizer.setDelimiter(",");
        
        DefaultLineMapper<Product> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> {
            log.info("Reading line: {}", fieldSet);
            
            Product product = new Product(
                fieldSet.readLong("product_id"),
                fieldSet.readLong("product_sku"),
                fieldSet.readString("product_name"),
                fieldSet.readLong("product_amount"),
                fieldSet.readString("product_data")
            );
            
            log.info("Created product: {}", product);
            return product;
        });
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public ProductItemProcessor processor() {
        return new ProductItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Product> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Product>()
                .sql("""
                    INSERT INTO products 
                    (productId, productSku, productNme, productAmount, productData)
                    VALUES (:productId, :productSku, :productName, :productAmount, :productData)
                    """)
                .dataSource(dataSource)
                .beanMapped()
                .build();
    }

    @Bean
    public Job importProductJob(JobRepository jobRepository, Step step1, JobCompletionNotificationListener listener) {
        return new JobBuilder("importProductJob", jobRepository)
                .listener(listener)
                .start(step1)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, DataSourceTransactionManager transactionManager,
                      FlatFileItemReader<Product> reader, ProductItemProcessor processor, JdbcBatchItemWriter<Product> writer) {
        return new StepBuilder("step1", jobRepository)
                .<Product, Product>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}