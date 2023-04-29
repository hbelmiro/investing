package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.Stock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
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
                Operation.builder()
                         .date(LocalDate.of(2019, 8, 27))
                         .stock(new Stock("BIDI11"))
                         .amount(BigDecimal.valueOf(8))
                         .price(BigDecimal.valueOf(57.99))
                         .tax(BigDecimal.valueOf(0.17))
                         .type(OperationType.SELL)
                        .build(),
                Operation.builder()
                         .date(LocalDate.of(2019, 9, 10))
                         .stock(new Stock("ITUB3"))
                         .amount(BigDecimal.valueOf(5))
                         .price(BigDecimal.valueOf(23.04))
                         .tax(BigDecimal.ZERO)
                         .type(OperationType.SELL)
                        .build(),
                Operation.builder()
                         .date(LocalDate.of(2019, 9, 12))
                         .stock(new Stock("LREN3"))
                         .amount(BigDecimal.valueOf(6))
                         .price(BigDecimal.valueOf(23.97))
                         .tax(BigDecimal.valueOf(0.15))
                         .type(OperationType.SELL)
                        .build(),
                Operation.builder()
                         .date(LocalDate.of(2019, 9, 12))
                         .stock(new Stock("MGLU3"))
                         .amount(BigDecimal.valueOf(23))
                         .price(BigDecimal.valueOf(8.35))
                         .tax(BigDecimal.valueOf(0.02))
                         .type(OperationType.SELL)
                        .build()
        };
    }

}