package com.hbelmiro.investing.operation.averageprice;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.Stock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

class AveragePriceCalculatorTest {

    @Test
    void calculate() {
        Stock abc = new Stock("abc");

        List<Operation> operations = List.of(
                Operation.builder()
                         .date(LocalDate.of(2021, 8, 2))
                         .stock(abc)
                         .amount(BigDecimal.TEN)
                         .price(BigDecimal.TEN)
                         .tax(new BigDecimal("0,1"))
                         .type(OperationType.BUY)
                         .build(),

                Operation.builder()
                         .date(LocalDate.of(2021, 8, 10))
                         .stock(abc)
                         .amount(BigDecimal.TEN)
                         .price(BigDecimal.TEN)
                         .tax(new BigDecimal("0,1"))
                         .type(OperationType.BUY)
                         .build()
        );

        fail("Not implemented yet");
    }
}