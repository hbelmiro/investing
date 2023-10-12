package com.hbelmiro.investing.operation.averageprice;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.asset.Asset;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.hbelmiro.investing.operation.averageprice.AveragePriceCalculator.DIFFERENT_PRICE_CURRENCY_ERROR_MSG;
import static com.hbelmiro.investing.operation.averageprice.AveragePriceCalculator.DIFFERENT_STOCK_ERROR_MSG;
import static com.hbelmiro.investing.operation.averageprice.AveragePriceCalculator.DIFFERENT_TAX_CURRENCY_ERROR_MSG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class AveragePriceCalculatorTest {

    @Inject
    AveragePriceCalculator averagePriceCalculator;

    private static final CurrencyUnit DOLLARS = Monetary.getCurrency("USD");

    private static final CurrencyUnit REAIS = Monetary.getCurrency("BRL");

    @Test
    void calculate() {
        Operation op1 = Operation.builder()
                .date(LocalDate.of(2020, 1, 1))
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(50, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.TEN)
                .build();

        Operation op2 = Operation.builder()
                .date(LocalDate.of(2020, 1, 2))
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(100, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.ONE)
                .build();

        Operation op3 = Operation.builder()
                .date(LocalDate.of(2020, 1, 3))
                .type(OperationType.SELL)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(75, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(new BigDecimal(5))
                .build();

        Operation op4 = Operation.builder()
                .date(LocalDate.of(2020, 1, 4))
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(75, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(new BigDecimal(100))
                .build();

        List<Operation> operations = List.of(op1, op2, op3, op4);

        assertThat(averagePriceCalculator.calculate(operations))
                .isEqualTo(Money.of(new BigDecimal("73.89"), DOLLARS));
    }

    @Test
    void calculateAssetNoLongerHold() {
        Operation op1 = Operation.builder()
                .date(LocalDate.of(2020, 1, 1))
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(50, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.TEN)
                .build();

        Operation op2 = Operation.builder()
                .date(LocalDate.of(2020, 1, 2))
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(100, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.ONE)
                .build();

        Operation op3 = Operation.builder()
                .date(LocalDate.of(2020, 1, 3))
                .type(OperationType.SELL)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(75, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(new BigDecimal(11))
                .build();

        List<Operation> operations = List.of(op1, op2, op3);

        assertThat(averagePriceCalculator.calculate(operations))
                .isEqualTo(Money.zero(DOLLARS));
    }

    @Test
    void operationsForNonDistinctStocksShouldThrowException() {
        Operation op1 = Operation.builder()
                .date(LocalDate.now())
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(12, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.TEN)
                .build();

        Operation op2 = Operation.builder()
                .date(LocalDate.now())
                .type(OperationType.BUY)
                .asset(new Asset("BBBB", DOLLARS))
                .price(Money.of(12, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.TEN)
                .build();

        List<Operation> operations = List.of(op1, op2);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> averagePriceCalculator.calculate(operations))
                .withMessage(DIFFERENT_STOCK_ERROR_MSG);
    }

    @Test
    void operationsForNonDistinctCurrencyPriceShouldThrowException() {
        Operation op1 = Operation.builder()
                .date(LocalDate.now())
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(12, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.TEN)
                .build();

        Operation op2 = Operation.builder()
                .date(LocalDate.now())
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(12, REAIS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.TEN)
                .build();

        List<Operation> operations = List.of(op1, op2);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> averagePriceCalculator.calculate(operations))
                .withMessage(DIFFERENT_PRICE_CURRENCY_ERROR_MSG);
    }

    @Test
    void operationsForNonDistinctCurrencyTaxShouldThrowException() {
        Operation op1 = Operation.builder()
                .date(LocalDate.now())
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(12, DOLLARS))
                .tax(Money.of(0.05, DOLLARS))
                .amount(BigDecimal.TEN)
                .build();

        Operation op2 = Operation.builder()
                .date(LocalDate.now())
                .type(OperationType.BUY)
                .asset(new Asset("AAAA", DOLLARS))
                .price(Money.of(12, DOLLARS))
                .tax(Money.of(0.05, REAIS))
                .amount(BigDecimal.TEN)
                .build();

        List<Operation> operations = List.of(op1, op2);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> averagePriceCalculator.calculate(operations))
                .withMessage(DIFFERENT_TAX_CURRENCY_ERROR_MSG);
    }
}