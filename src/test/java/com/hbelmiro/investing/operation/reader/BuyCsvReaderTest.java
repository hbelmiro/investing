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

    private Operation[] expectedOperations() {
        return new Operation[]{
                new Operation(LocalDate.of(2019, 4, 24), new Stock("ITUB3"), BigDecimal.valueOf(3), BigDecimal.valueOf(29.39), BigDecimal.ZERO, OperationType.BUY),
                new Operation(LocalDate.of(2019, 6, 5), new Stock("LREN3"), BigDecimal.valueOf(1), BigDecimal.valueOf(14.44), BigDecimal.ZERO, OperationType.BUY),
                new Operation(LocalDate.of(2019, 7, 4), new Stock("BIDI11"), BigDecimal.valueOf(40), BigDecimal.ZERO, BigDecimal.ZERO, OperationType.BUY),
                new Operation(LocalDate.of(2019, 9, 10), new Stock("MGLU3"), BigDecimal.valueOf(3), BigDecimal.valueOf(31.9), BigDecimal.valueOf(.05), OperationType.BUY),
        };
    }

}