package com.hbelmiro.investing.utils;

import com.hbelmiro.investing.currency.CurrencyCode;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;

public final class MoneyUtil {

    public static final CurrencyUnit BRL = Monetary.getCurrency(CurrencyCode.BRL);
    public static final CurrencyUnit USD = Monetary.getCurrency(CurrencyCode.USD);

    private MoneyUtil() {
    }

    public static Money toMoney(String value, CurrencyUnit currencyUnit) {
        if (value.isEmpty()) {
            return Money.zero(currencyUnit);
        }

        String sanitizedValue = value.replace(" ", "")
                .replace(getSymbol(currencyUnit), "")
                .replace(".", "")
                .replace(",", ".");

        return Money.of(new BigDecimal(sanitizedValue), currencyUnit);
    }

    public static Money toBrazilianMoney(String value) {
        return toMoney(value, BRL);
    }

    public static Money toMoney(String value, String currencyCode) {
        return toMoney(value, Monetary.getCurrency(currencyCode));
    }

    static String getSymbol(CurrencyUnit currencyUnit) {
        return switch (currencyUnit.getCurrencyCode()) {
            case CurrencyCode.BRL -> "R$";
            case CurrencyCode.USD -> "$";
            default -> throw new UnsupportedOperationException("Unsupported currency unit: " + currencyUnit);
        };
    }
}
