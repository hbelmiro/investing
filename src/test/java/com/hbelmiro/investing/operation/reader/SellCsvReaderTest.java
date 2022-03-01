package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.Stock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SellCsvReaderTest {

    @Inject
    SellCsvReader reader;

    @Test
    void read() {
        assertThat(reader.read()).containsExactlyInAnyOrder(expectedOperations());
    }

    private static Operation[] expectedOperations() {
        return new Operation[]{
                new Operation(LocalDate.of(2019, 8, 27), new Stock("BIDI11"), BigDecimal.valueOf(8), BigDecimal.valueOf(57.99), BigDecimal.valueOf(0.17), OperationType.SELL),
                new Operation(LocalDate.of(2019, 9, 10), new Stock("ITUB3"), BigDecimal.valueOf(5), BigDecimal.valueOf(23.04), BigDecimal.ZERO, OperationType.SELL),
                new Operation(LocalDate.of(2019, 9, 12), new Stock("LREN3"), BigDecimal.valueOf(6), BigDecimal.valueOf(23.97), BigDecimal.valueOf(0.15), OperationType.SELL),
                new Operation(LocalDate.of(2019, 9, 12), new Stock("MGLU3"), BigDecimal.valueOf(23), BigDecimal.valueOf(8.35), BigDecimal.valueOf(0.02), OperationType.SELL),
        };
    }

}