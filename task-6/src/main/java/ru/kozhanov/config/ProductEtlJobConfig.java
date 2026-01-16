package ru.kozhanov.config;

import ru.kozhanov.model.ProductCsvRow;
import ru.kozhanov.model.ProductUpsertRow;
import ru.kozhanov.processor.LoyaltyEnrichmentProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class ProductEtlJobConfig {

    @Value("${app.input-file}")
    private String inputFile;

    @Value("${app.chunk-size:1000}")
    private int chunkSize;

    @Bean
    public FlatFileItemReader<ProductCsvRow> productCsvReader() {
        return new FlatFileItemReaderBuilder<ProductCsvRow>()
                .name("productCsvReader")
                .resource(new FileSystemResource(inputFile))
                .delimited()
                .delimiter(",")
                .names("sku", "name", "category", "base_price", "loyalty_code")
                .fieldSetMapper(fieldSet -> {
                    ProductCsvRow r = new ProductCsvRow();
                    r.setSku(fieldSet.readString("sku"));
                    r.setName(fieldSet.readString("name"));
                    r.setCategory(fieldSet.readString("category"));
                    r.setBasePrice(fieldSet.readDouble("base_price"));
                    r.setLoyaltyCode(fieldSet.readString("loyalty_code"));
                    return r;
                })
                .linesToSkip(1)
                .build();
    }

    @Bean
    public LoyaltyEnrichmentProcessor loyaltyProcessor(JdbcTemplate jdbcTemplate) {
        return new LoyaltyEnrichmentProcessor(jdbcTemplate);
    }

    @Bean
    public JdbcBatchItemWriter<ProductUpsertRow> productWriter(DataSource dataSource) {
        JdbcBatchItemWriter<ProductUpsertRow> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql(
                "INSERT INTO products (sku, name, category, base_price, loyalty_code, discount_percent, final_price, updated_at) " +
                        "VALUES (:sku, :name, :category, :basePrice, :loyaltyCode, :discountPercent, :finalPrice, NOW()) " +
                        "ON CONFLICT (sku) DO UPDATE SET " +
                        "name = EXCLUDED.name, " +
                        "category = EXCLUDED.category, " +
                        "base_price = EXCLUDED.base_price, " +
                        "loyalty_code = EXCLUDED.loyalty_code, " +
                        "discount_percent = EXCLUDED.discount_percent, " +
                        "final_price = EXCLUDED.final_price, " +
                        "updated_at = NOW()"
        );
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        return writer;
    }

    @Bean
    public Step productEtlStep(StepBuilderFactory steps,
                               FlatFileItemReader<ProductCsvRow> reader,
                               LoyaltyEnrichmentProcessor processor,
                               JdbcBatchItemWriter<ProductUpsertRow> writer) {
        return steps.get("productEtlStep")
                .<ProductCsvRow, ProductUpsertRow>chunk(chunkSize)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job productEtlJob(JobBuilderFactory jobs, Step productEtlStep) {
        return jobs.get("productEtlJob")
                .start(productEtlStep)
                .build();
    }
}
