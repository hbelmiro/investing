package com.hbelmiro.investing.utils;

import com.hbelmiro.investing.currency.CurrencyCode;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;

public final class MoneyUtil {

    public static final CurrencyUnit BRL = Monetary.getCurrency(CurrencyCode.BRL);

    private MoneyUtil() {
    }

    public static Money toMoney(String value, CurrencyUnit currencyUnit) {
        var bigDecimal = new BigDecimal(value.replace("R$ ", "").replace(".", "").replace(",", "."));
        return Money.of(bigDecimal, currencyUnit);
    }

    public static Money toBrazilianMoney(String value) {
        return toMoney(value, BRL);
    }

    public static Money toMoney(String value, String currencyCode) {
        return toMoney(value, Monetary.getCurrency(currencyCode));
    }
}
