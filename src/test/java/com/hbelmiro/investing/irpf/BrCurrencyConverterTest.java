package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.dividend.DividendType;
import com.hbelmiro.investing.utils.MoneyUtil;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BrCurrencyConverterTest {

    private final BrCurrencyConverter converter = new BrCurrencyConverter();

    private static final Asset PETR4 = new Asset("PETR4", MoneyUtil.BRL);

    @Test
    void toCostBrl() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(PETR4)
                .price(Money.of(new BigDecimal("30.00"), MoneyUtil.BRL))
                .tax(Money.of(new BigDecimal("0.50"), MoneyUtil.BRL))
                .amount(BigDecimal.TEN)
                .build();

        Money result = converter.toCostBrl(buy);

        assertThat(result).isEqualTo(Money.of(new BigDecimal("300.50"), MoneyUtil.BRL));
    }

    @Test
    void toSellBrl() {
        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 6, 15))
                .type(OperationType.SELL)
                .asset(PETR4)
                .price(Money.of(new BigDecimal("40.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(new BigDecimal("5"))
                .build();

        Money result = converter.toSellBrl(sell);

        assertThat(result).isEqualTo(Money.of(new BigDecimal("200.00"), MoneyUtil.BRL));
    }

    @Test
    void toDividendGrossBrl() {
        Dividend dividend = new Dividend(
                LocalDate.of(2025, 3, 15), DividendType.DIVIDEND,
                Money.of(new BigDecimal("2.50"), MoneyUtil.BRL),
                Money.of(new BigDecimal("0.30"), MoneyUtil.BRL), PETR4);

        assertThat(converter.toDividendGrossBrl(dividend))
                .isEqualTo(Money.of(new BigDecimal("2.50"), MoneyUtil.BRL));
    }

    @Test
    void toDividendTaxBrl() {
        Dividend dividend = new Dividend(
                LocalDate.of(2025, 3, 15), DividendType.DIVIDEND,
                Money.of(new BigDecimal("2.50"), MoneyUtil.BRL),
                Money.of(new BigDecimal("0.30"), MoneyUtil.BRL), PETR4);

        assertThat(converter.toDividendTaxBrl(dividend))
                .isEqualTo(Money.of(new BigDecimal("0.30"), MoneyUtil.BRL));
    }
}
