package com.hbelmiro.investing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class Operation {

    private final LocalDate date;
    private final Stock stock;
    private final BigDecimal amount;
    private final BigDecimal price;
    private final BigDecimal tax;
    private final OperationType type;

    private Operation(Builder builder) {
        date = builder.date;
        stock = builder.stock;
        amount = builder.amount;
        price = builder.price;
        tax = builder.tax;
        type = builder.type;
    }

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

    public LocalDate getDate() {
        return date;
    }

    public Stock getStock() {
        return stock;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public OperationType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Operation[" +
                "date=" + date + ", " +
                "stock=" + stock + ", " +
                "amount=" + amount + ", " +
                "price=" + price + ", " +
                "tax=" + tax + ", " +
                "type=" + type + ']';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate date;
        private Stock stock;
        private BigDecimal amount;
        private BigDecimal price;
        private BigDecimal tax;
        private OperationType type;

        private Builder() {
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder stock(Stock stock) {
            this.stock = stock;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder tax(BigDecimal tax) {
            this.tax = tax;
            return this;
        }

        public Builder type(OperationType type) {
            this.type = type;
            return this;
        }

        public Operation build() {
            return new Operation(this);
        }
    }

}
