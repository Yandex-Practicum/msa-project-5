package com.example.batchprocessing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.ClassPathResource;

class ProductCsvReaderTest {

    @Test
    void readsAllProductsFromCsv() throws Exception {
        FlatFileItemReader<Product> reader = new BatchConfiguration().reader(
                new ClassPathResource("product-data.csv")
        );
        reader.open(new ExecutionContext());

        Product first = reader.read();
        int count = 1;
        while (reader.read() != null) {
            count++;
        }
        reader.close();

        assertThat(first).isEqualTo(new Product(1L, 20001L, "hammer", 45L, "Loyality_off"));
        assertThat(count).isEqualTo(5);
    }
}
