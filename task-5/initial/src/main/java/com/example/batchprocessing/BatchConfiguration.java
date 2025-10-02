package com.example.batchprocessing;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
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
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public FlatFileItemReader<Product> productReader() {
        return new FlatFileItemReaderBuilder<Product>()
                .name("productItemReader")
                .resource(new ClassPathResource("product-data.csv"))
                .delimited()
                .names(new String[] {"productId", "productSku", "productName", "productAmount", "productData"})
                .fieldSetMapper(fieldSet -> new Product(
                        fieldSet.readLong("productId"),
                        fieldSet.readLong("productSku"),
                        fieldSet.readString("productName"),
                        fieldSet.readLong("productAmount"),
                        fieldSet.readString("productData")
                ))
                .build();
    }

    @Bean
    public ProductItemProcessor processor() {
        return new ProductItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Product> productWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Product>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO products (productId, productSku, productName, productAmount, productData) VALUES (:productId, :productSku, :productName, :productAmount, :productData) ON CONFLICT (productId) DO UPDATE SET productSku = EXCLUDED.productSku, productName = EXCLUDED.productName, productAmount = EXCLUDED.productAmount, productData = EXCLUDED.productData")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public FlatFileItemReader<Loyality> loyaltyReader() {
        return new FlatFileItemReaderBuilder<Loyality>()
                .name("loyaltyItemReader")
                .resource(new ClassPathResource("loyality_data.csv"))
                .delimited()
                .names(new String[] {"productSku", "loyalityData"})
                .fieldSetMapper(fieldSet -> new Loyality(
                        fieldSet.readLong("productSku"),
                        fieldSet.readString("loyalityData")
                ))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Loyality> loyaltyWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Loyality>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO loyality_data (productSku, loyalityData) VALUES (:productSku, :loyalityData) ON CONFLICT (productSku) DO UPDATE SET loyalityData = EXCLUDED.loyalityData")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step step1(
            JobRepository jobRepository,
            DataSourceTransactionManager transactionManager,
            FlatFileItemReader<Loyality> loyaltyReader,
            JdbcBatchItemWriter<Loyality> loyaltyWriter
    ) {
        return new StepBuilder("step1", jobRepository)
                .<Loyality, Loyality>chunk(10, transactionManager)
                .reader(loyaltyReader)
                .writer(loyaltyWriter)
                .build();
    }

    @Bean
    public Step step2(
            JobRepository jobRepository,
            DataSourceTransactionManager transactionManager,
            FlatFileItemReader<Product> productReader,
            ProductItemProcessor processor,
            JdbcBatchItemWriter<Product> productWriter
    ) {
        return new StepBuilder("step2", jobRepository)
                .<Product, Product>chunk(10, transactionManager)
                .reader(productReader)
                .processor(processor)
                .writer(productWriter)
                .build();
    }

    @Bean
    public Job importProductJob(JobRepository jobRepository, Step step2, Step step1, JobCompletionNotificationListener listener) {
        return new JobBuilder("importProductJob", jobRepository)
                .listener(listener)
                .start(step1) // Сначала загружаем данные лояльности
                .next(step2)  // Затем обрабатываем продукты
                .build();
    }

}
