package com.hbelmiro.investing.operation.averageprice;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.asset.Asset;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
final class AveragePriceCalculator {

    public static final String DIFFERENT_STOCK_ERROR_MSG = "Operations must be from the same stock";

    public static final String DIFFERENT_PRICE_CURRENCY_ERROR_MSG = "Prices must be all in the same currency";

    public static final String DIFFERENT_TAX_CURRENCY_ERROR_MSG = "Taxes must be all in the same currency";

    AveragePriceCalculator() {
    }

    Money calculate(List<Operation> operations) {
        validate(operations);

        Money totalPrice = Money.zero(operations.get(0).getPrice().getCurrency());
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<Operation> sortedOperations = operations.stream().sorted(Comparator.comparing(Operation::getDate)).toList();

        for (Operation op : sortedOperations) {
            if (op.getType() == OperationType.BUY) {
                Money opValue = op.getPrice().multiply(op.getAmount()).add(op.getTax());
                totalPrice = totalPrice.add(opValue);
                totalAmount = totalAmount.add(op.getAmount());
            } else {
                Money currentAverage = totalPrice.divide(totalAmount);
                totalPrice = totalPrice.subtract(currentAverage.multiply(op.getAmount()));
                totalAmount = totalAmount.subtract(op.getAmount());
            }
        }

        return totalPrice.divide(totalAmount);
    }

    private static void validate(List<Operation> operations) {
        if (operations.stream()
                .map(Operation::getStock)
                .map(Asset::symbol)
                .distinct()
                .count() > 1) {
            throw new IllegalArgumentException(DIFFERENT_STOCK_ERROR_MSG);
        }

        if (operations.stream()
                .map(Operation::getPrice)
                .map(Money::getCurrency)
                .distinct()
                .count() > 1) {
            throw new IllegalArgumentException(DIFFERENT_PRICE_CURRENCY_ERROR_MSG);
        }

        if (operations.stream()
                .map(Operation::getTax)
                .map(Money::getCurrency)
                .distinct()
                .count() > 1) {
            throw new IllegalArgumentException(DIFFERENT_TAX_CURRENCY_ERROR_MSG);
        }
    }
}
