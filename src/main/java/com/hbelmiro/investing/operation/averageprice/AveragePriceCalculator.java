package com.hbelmiro.investing.operation.averageprice;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.Stock;

import java.math.BigDecimal;
import java.util.List;

final class AveragePriceCalculator {

    private AveragePriceCalculator() {
    }

    BigDecimal calculate(List<Operation> operations) {
        validate(operations);

        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static void validate(List<Operation> operations) {
        if (operations.stream()
                      .map(Operation::getStock)
                      .map(Stock::symbol)
                      .distinct()
                      .count() > 1) {
            throw new IllegalArgumentException("Operations must be from the same stock");
        }
    }
}
