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
class BuyCsvReaderTest {

    @Inject
    BuyCsvReader reader;

    @Test
    void read() {
        assertThat(reader.read()).containsExactlyInAnyOrder(expectedOperations());
    }

    private static Operation[] expectedOperations() {
        return new Operation[]{
                Operation.builder()
                         .date(LocalDate.of(2019, 4, 24))
                         .stock(new Stock("ITUB3"))
                         .amount(BigDecimal.valueOf(3))
                         .price(BigDecimal.valueOf(29.39))
                         .tax(BigDecimal.ZERO)
                         .type(OperationType.BUY)
                        .build(),
                Operation.builder()
                         .date(LocalDate.of(2019, 5, 6))
                         .stock(new Stock("LREN3"))
                         .amount(BigDecimal.valueOf(1))
                         .price(BigDecimal.valueOf(14.44))
                         .tax(BigDecimal.ZERO)
                         .type(OperationType.BUY)
                        .build(),
                Operation.builder()
                         .date(LocalDate.of(2019, 7, 4))
                         .stock(new Stock("BIDI11"))
                         .amount(BigDecimal.valueOf(40))
                         .price(BigDecimal.ZERO)
                         .tax(BigDecimal.ZERO)
                         .type(OperationType.BUY)
                        .build(),
                Operation.builder()
                         .date(LocalDate.of(2019, 9, 10))
                         .stock(new Stock("MGLU3"))
                         .amount(BigDecimal.valueOf(3))
                         .price(BigDecimal.valueOf(31.9))
                         .tax(BigDecimal.valueOf(.05))
                         .type(OperationType.BUY)
                        .build()
        };
    }

}