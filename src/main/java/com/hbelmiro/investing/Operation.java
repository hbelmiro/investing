package com.hbelmiro.investing;

import com.hbelmiro.investing.asset.Asset;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class Operation {

    private final LocalDate date;
    private final Asset asset;
    private final BigDecimal amount;
    private final Money price;
    private final Money tax;
    private final OperationType type;

    private Operation(Builder builder) {
        date = builder.date;
        asset = builder.asset;
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
                asset.equals(operation.asset) &&
                amount.compareTo(operation.amount) == 0 &&
                price.compareTo(operation.price) == 0 &&
                tax.compareTo(operation.tax) == 0 &&
                type == operation.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, asset, amount, price, tax, type);
    }

    public LocalDate getDate() {
        return date;
    }

    public Asset getStock() {
        return asset;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Money getPrice() {
        return price;
    }

    public Money getTax() {
        return tax;
    }

    public OperationType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Operation[" +
                "date=" + date + ", " +
                "stock=" + asset + ", " +
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
        private Asset asset;
        private BigDecimal amount;
        private Money price;
        private Money tax;
        private OperationType type;

        private Builder() {
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder stock(Asset asset) {
            this.asset = asset;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder price(Money price) {
            this.price = price;
            return this;
        }

        public Builder tax(Money tax) {
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
