package com.hbelmiro.investing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Operation(LocalDate date, Stock stock, BigDecimal amount, BigDecimal price, BigDecimal tax,
                        OperationType type) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var operation = (Operation) o;
        return date.equals(operation.date) &&
                stock.equals(operation.stock) &&
                amount.compareTo(operation.amount) == 0 &&
                price.compareTo(operation.price) == 0 &&
                tax.compareTo(operation.tax) == 0 &&
                type == operation.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, stock, amount, price, tax, type);
    }
}
