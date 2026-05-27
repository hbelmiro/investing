package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.dividend.DividendType;
import com.hbelmiro.investing.ptax.FakePtaxService;
import com.hbelmiro.investing.utils.MoneyUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class IrpfCalculatorTest {

    @Inject
    IrpfCalculator irpfCalculator;

    @Inject
    FakePtaxService fakePtaxService;

    private CurrencyConverter usConverter;

    private static final Asset AAPL = new Asset("AAPL", MoneyUtil.USD);

    private static final Operation BUY_JAN = Operation.builder()
            .date(LocalDate.of(2025, 1, 15)).type(OperationType.BUY).asset(AAPL)
            .price(Money.of(50, MoneyUtil.USD)).tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
            .amount(BigDecimal.TEN).build();

    private static final Operation SELL_FEB = Operation.builder()
            .date(LocalDate.of(2025, 2, 15)).type(OperationType.SELL).asset(AAPL)
            .price(Money.of(55, MoneyUtil.USD)).tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
            .amount(new BigDecimal("5")).build();

    private static final Operation BUY_MAR = Operation.builder()
            .date(LocalDate.of(2025, 3, 15)).type(OperationType.BUY).asset(AAPL)
            .price(Money.of(60, MoneyUtil.USD)).tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
            .amount(new BigDecimal("10")).build();

    private static final Operation SELL_JUN = Operation.builder()
            .date(LocalDate.of(2025, 6, 15)).type(OperationType.SELL).asset(AAPL)
            .price(Money.of(70, MoneyUtil.USD)).tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
            .amount(new BigDecimal("5")).build();

    private static final Dividend DIV_JAN = new Dividend(
            LocalDate.of(2025, 1, 15), DividendType.DIVIDEND,
            Money.of(new BigDecimal("0.75"), MoneyUtil.USD),
            Money.of(new BigDecimal("0.05"), MoneyUtil.USD), AAPL);

    private static final Dividend DIV_JUN = new Dividend(
            LocalDate.of(2025, 6, 15), DividendType.DIVIDEND,
            Money.of(new BigDecimal("1.20"), MoneyUtil.USD),
            Money.of(BigDecimal.ZERO, MoneyUtil.USD), AAPL);

    @BeforeEach
    void setUp() {
        fakePtaxService.setRate(LocalDate.of(2025, 1, 15), Money.of(new BigDecimal("6.0370"), MoneyUtil.BRL), Money.of(new BigDecimal("6.0380"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 2, 15), Money.of(new BigDecimal("5.8000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.8100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 3, 15), Money.of(new BigDecimal("5.5000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.5100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 6, 15), Money.of(new BigDecimal("5.7000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.7100"), MoneyUtil.BRL));
        usConverter = new UsCurrencyConverter(fakePtaxService);
    }

    // --- calculateCapitalGains ---

    // | Date       | Op   | Qty | Price | Tax  | PTAX Compra | PTAX Venda |
    // |------------|------|-----|-------|------|-------------|------------|
    // | 2025-01-15 | BUY  | 10  | $50   | $0.50| 6.0370      |            |
    // | 2025-06-15 | SELL | 5   | $70   | $0.35|             | 5.7100     |
    //
    // | Result              | Value   |
    // |---------------------|---------|
    // | avgCostBrl          | 302.15  |
    // | sellBrl             | 1998.50 |
    // | costBrl (5 shares)  | 1510.76 |
    // | capitalGainsBrl     | 487.74  |
    @Test
    void calculateCapitalGains_sellWithGain() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(50, MoneyUtil.USD))
                .tax(Money.of(new BigDecimal("0.50"), MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 6, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(70, MoneyUtil.USD))
                .tax(Money.of(new BigDecimal("0.35"), MoneyUtil.USD))
                .amount(new BigDecimal("5"))
                .build();

        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, usConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("487.74"), MoneyUtil.BRL));
    }

    // | Date       | Op   | Qty | Price | Tax | PTAX Compra | PTAX Venda |
    // |------------|------|-----|-------|-----|-------------|------------|
    // | 2025-01-15 | BUY  | 10  | $100  | $0  | 6.0370      |            |
    // | 2025-06-15 | SELL | 5   | $80   | $0  |             | 5.7100     |
    //
    // | Result              | Value    |
    // |---------------------|----------|
    // | avgCostBrl          | 603.70   |
    // | sellBrl             | 2284.00  |
    // | costBrl (5 shares)  | 3018.50  |
    // | capitalGainsBrl     | -734.50  |
    @Test
    void calculateCapitalGains_sellWithLoss() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(100, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 6, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(80, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("5"))
                .build();

        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, usConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("-734.50"), MoneyUtil.BRL));
    }

    // | Date       | Op  | Qty | Price | PTAX Compra |
    // |------------|-----|-----|-------|-------------|
    // | 2025-01-15 | BUY | 10  | $50   | 6.0370      |
    //
    // | Result          | Value  |
    // |-----------------|--------|
    // | capitalGainsBrl | 0      |
    // | avgCostBrl      | 301.85 |
    // | avgCostUsd      | 50.00  |
    @Test
    void calculateCapitalGains_noSells() {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(BUY_JAN), List.of(), 2025, usConverter);

        assertThat(result.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("301.85"), MoneyUtil.BRL));
        assertThat(result.avgCostOriginal().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.USD));
    }

    // | Date       | Op   | Qty | Price | PTAX Compra | PTAX Venda |
    // |------------|------|-----|-------|-------------|------------|
    // | 2025-01-15 | BUY  | 10  | $50   | 6.0370      |            |
    // | 2025-02-15 | SELL | 5   | $55   |             | 5.8100     |
    // | 2025-03-15 | BUY  | 10  | $60   | 5.5000      |            |
    // | 2025-06-15 | SELL | 5   | $70   |             | 5.7100     |
    //
    // | Sell       | Avg at sell date      | SellBrl  | CostBrl  | Gain    |
    // |------------|-----------------------|----------|----------|---------|
    // | 2025-02-15 | 3018.50/10 = 301.85   | 1597.75  | 1509.25  | 88.50   |
    // | 2025-06-15 | 6318.50/20 = 315.925  | 1998.50  | 1579.63  | 418.88  |
    //
    // | Result          | Value  |
    // |-----------------|--------|
    // | capitalGainsBrl | 507.38 |
    // | avgCostBrl      | 315.92 |
    // | avgCostUsd      | 55.00  |
    static Stream<Arguments> multipleBuysAndSellsArgs() {
        return Stream.of(
                Arguments.of(Named.of("chronological order", List.of(BUY_JAN, BUY_MAR)), List.of(SELL_FEB, SELL_JUN)),
                Arguments.of(Named.of("reversed order", List.of(BUY_MAR, BUY_JAN)), List.of(SELL_JUN, SELL_FEB))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multipleBuysAndSellsArgs")
    void calculateCapitalGains_multipleBuysAndSells(List<Operation> buys, List<Operation> sells) {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(buys, sells, 2025, usConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("507.38"), MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("315.92"), MoneyUtil.BRL));
        assertThat(result.avgCostOriginal().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("55.00"), MoneyUtil.USD));
    }

    // | Date       | Op   | Qty | Price |
    // |------------|------|-----|-------|
    // | 2025-01-15 | BUY  | 10  | $50   |
    // | 2025-06-15 | SELL | 11  | $70   |
    // → error: sell exceeds position
    @Test
    void calculateCapitalGains_sellMoreThanHeld() {
        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 6, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(70, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("11"))
                .build();

        List<Operation> buys = List.of(BUY_JAN);
        List<Operation> sells = List.of(sell);
        var calculator = irpfCalculator;
        var converter = usConverter;
        var year = 2025;
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> calculator.calculateCapitalGains(buys, sells, year, converter));
    }

    // | Date       | Op   | Qty | Price |
    // |------------|------|-----|-------|
    // | 2025-01-15 | SELL | 5   | $70   |
    // → error: no buys
    @Test
    void calculateCapitalGains_sellBeforeBuy() {
        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(70, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("5"))
                .build();

        List<Operation> buys = List.of();
        List<Operation> sells = List.of(sell);
        var calculator = irpfCalculator;
        var converter = usConverter;
        var year = 2025;
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> calculator.calculateCapitalGains(buys, sells, year, converter));
    }

    // | Date       | Op   | Qty | Price |
    // |------------|------|-----|-------|
    // | 2025-02-15 | SELL | 5   | $55   |
    // | 2025-03-15 | BUY  | 10  | $60   |
    // → error: at sell date, 0 shares bought
    @Test
    void calculateCapitalGains_sellExceedsCumulativeBoughtAtThatDate() {
        List<Operation> buys = List.of(BUY_MAR);
        List<Operation> sells = List.of(SELL_FEB);
        var calculator = irpfCalculator;
        var converter = usConverter;
        var year = 2025;
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> calculator.calculateCapitalGains(buys, sells, year, converter));
    }

    // | Date       | Op   | Qty | Price | PTAX Compra | PTAX Venda |
    // |------------|------|-----|-------|-------------|------------|
    // | 2025-01-15 | BUY  | 10  | $50   | 6.0370      |            |
    // | 2025-01-15 | SELL | 5   | $55   |             | 6.0380     |
    //
    // | Result          | Value  |
    // |-----------------|--------|
    // | avgCostBrl      | 301.85 |
    // | sellBrl         | 1660.45|
    // | capitalGainsBrl | 151.20 |
    @Test
    void calculateCapitalGains_sameDateBuyAndSell() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(50, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(55, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("5"))
                .build();

        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, usConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("151.20"), MoneyUtil.BRL));
    }

    // | Date       | Op   | Qty | Price | PTAX Compra | PTAX Venda |
    // |------------|------|-----|-------|-------------|------------|
    // | 2025-01-15 | BUY  | 10  | $50   | 6.0370      |            |
    // | 2025-06-15 | SELL | 10  | $70   |             | 5.7100     |
    //
    // | Result          | Value  |
    // |-----------------|--------|
    // | avgCostBrl      | 301.85 |
    // | capitalGainsBrl | 978.50 |
    @Test
    void calculateCapitalGains_sellAllShares() {
        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 6, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(70, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(BUY_JAN), List.of(sell), 2025, usConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("978.50"), MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("301.85"), MoneyUtil.BRL));
    }

    // --- Multi-year capital gains matrix (target year=2025) ---
    //
    // | Constant   | Date       | Op   | Qty | Price | PTAX Compra | PTAX Venda |
    // |------------|------------|------|-----|-------|-------------|------------|
    // | BUY_2024   | 2024-06-15 | BUY  | 10  | $50   | 5.4000      |            |
    // | SELL_2024  | 2024-09-15 | SELL | 3   | $55   |             | 5.4600     |
    // | BUY_2025   | 2025-01-15 | BUY  | 10  | $50   | 6.0370      |            |
    // | SELL_2025  | 2025-06-15 | SELL | 5   | $70   |             | 5.7100     |
    // | BUY_2026   | 2026-03-15 | BUY  | 8   | $65   | 5.9000      |            |
    // | SELL_2026  | 2026-06-15 | SELL | 4   | $75   |             | 5.8000     |

    private static final Operation BUY_2024 = Operation.builder()
            .date(LocalDate.of(2024, 6, 15)).type(OperationType.BUY).asset(AAPL)
            .price(Money.of(50, MoneyUtil.USD)).tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
            .amount(BigDecimal.TEN).build();

    private static final Operation SELL_2024 = Operation.builder()
            .date(LocalDate.of(2024, 9, 15)).type(OperationType.SELL).asset(AAPL)
            .price(Money.of(55, MoneyUtil.USD)).tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
            .amount(new BigDecimal("3")).build();

    private static final Operation BUY_2025 = BUY_JAN;

    private static final Operation SELL_2025 = SELL_JUN;

    private static final Operation BUY_2026 = Operation.builder()
            .date(LocalDate.of(2026, 3, 15)).type(OperationType.BUY).asset(AAPL)
            .price(Money.of(65, MoneyUtil.USD)).tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
            .amount(new BigDecimal("8")).build();

    private static final Operation SELL_2026 = Operation.builder()
            .date(LocalDate.of(2026, 6, 15)).type(OperationType.SELL).asset(AAPL)
            .price(Money.of(75, MoneyUtil.USD)).tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
            .amount(new BigDecimal("4")).build();

    // #1 | 2025: B | Expected: yearGains=0, totalGains=0, avgUsd=50.00
    @Test
    void multiYear_onlyBuy2025() {
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2025), List.of(), 2025, usConverter);
        assertThat(r.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.avgCostOriginal().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.USD));
    }

    // #2 | 2025: B+S | Expected: yearGains=489.25, yearGains=totalGains
    @Test
    void multiYear_buyAndSell2025() {
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2025), List.of(SELL_2025), 2025, usConverter);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("489.25"), MoneyUtil.BRL));
        assertThat(r.capitalGainsBrl()).isEqualTo(r.totalCapitalGainsBrl());
    }

    // #3 | 2025: S only | Expected: error (no buys)
    @Test
    void multiYear_onlySell2025_nobuys() {
        List<Operation> buys = List.of();
        List<Operation> sells = List.of(SELL_2025);
        var calculator = irpfCalculator;
        var converter = usConverter;
        var year = 2025;
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> calculator.calculateCapitalGains(buys, sells, year, converter));
    }

    // #4 | 2024: B | 2025: B | Expected: yearGains=0, totalGains=0, avgUsd=50.00
    @Test
    void multiYear_buy2024_buy2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025), List.of(), 2025, usConverter);
        assertThat(r.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.avgCostOriginal().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.USD));
    }

    // #5 | 2024: B | 2025: S | Expected: yearGains=648.50, yearGains=totalGains
    @Test
    void multiYear_buy2024_sell2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024), List.of(SELL_2025), 2025, usConverter);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("648.50"), MoneyUtil.BRL));
        assertThat(r.capitalGainsBrl()).isEqualTo(r.totalCapitalGainsBrl());
    }

    // #6 | 2024: B | 2025: B+S | Expected: yearGains=568.88, yearGains=totalGains
    @Test
    void multiYear_buy2024_buyAndSell2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025), List.of(SELL_2025), 2025, usConverter);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.capitalGainsBrl()).isEqualTo(r.totalCapitalGainsBrl());
    }

    // #7 | 2024: B+S | 2025: B
    //
    // | Sell       | Avg at sell date (buys-only) | SellBrl | CostBrl | Gain  | In year? |
    // |------------|-----------------------------|---------|---------|-------|----------|
    // | 2024-09-15 | 2700/10 = 270               | 900.90  | 810.00  | 90.90 | no       |
    //
    // Expected: yearGains=0, totalGains=90.90
    @Test
    void multiYear_buyAndSell2024_buy2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025), List.of(SELL_2024), 2025, usConverter);
        assertThat(r.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("90.90"), MoneyUtil.BRL));
    }

    // #8 | 2024: B+S | 2025: S
    //
    // | Sell       | Avg at sell date | SellBrl  | CostBrl | Gain   | In year? |
    // |------------|-----------------|----------|---------|--------|----------|
    // | 2024-09-15 | 270             | 900.90   | 810.00  | 90.90  | no       |
    // | 2025-06-15 | 270             | 1998.50  | 1350.00 | 648.50 | yes      |
    //
    // Expected: yearGains=648.50, totalGains=739.40
    @Test
    void multiYear_buyAndSell2024_sell2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024), List.of(SELL_2024, SELL_2025), 2025, usConverter);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("648.50"), MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("739.40"), MoneyUtil.BRL));
    }

    // #9 | 2024: B+S | 2025: B+S
    //
    // | Sell       | Avg at sell date          | SellBrl  | CostBrl  | Gain   | In year? |
    // |------------|--------------------------|----------|----------|--------|----------|
    // | 2024-09-15 | BUY_2024 only = 270      | 900.90   | 810.00   | 90.90  | no       |
    // | 2025-06-15 | BUY_2024+BUY_2025=285.93 | 1998.50  | 1429.63  | 568.88 | yes      |
    //
    // Expected: yearGains=568.88, totalGains=659.78
    @Test
    void multiYear_buyAndSell2024_buyAndSell2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025), List.of(SELL_2024, SELL_2025), 2025, usConverter);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("659.78"), MoneyUtil.BRL));
    }

    // #10 | 2024: B | 2025: B+S | 2026: B (excluded)
    // BUY_2026 excluded from avg. Expected: yearGains=568.88, avgUsd=50.00
    @Test
    void multiYear_buy2024_buyAndSell2025_buy2026excluded() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 3, 15), Money.of(new BigDecimal("5.9000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.9100"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025, BUY_2026), List.of(SELL_2025), 2025, usConverter);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.avgCostOriginal().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.USD));
    }

    // #11 | 2024: B | 2025: B+S | 2026: B+S (both excluded)
    // Expected: yearGains=568.88, yearGains=totalGains
    @Test
    void multiYear_buy2024_buyAndSell2025_buyAndSell2026excluded() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 3, 15), Money.of(new BigDecimal("5.9000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.9100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 6, 15), Money.of(new BigDecimal("5.8000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.8100"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025, BUY_2026), List.of(SELL_2025, SELL_2026), 2025, usConverter);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.capitalGainsBrl()).isEqualTo(r.totalCapitalGainsBrl());
    }

    // #12 | 2024: B+S | 2025: B+S | 2026: B+S (excluded)
    //
    // | Sell       | Avg at sell date     | Gain   | In year? |
    // |------------|---------------------|--------|----------|
    // | 2024-09-15 | BUY_2024 only = 270 | 90.90  | no       |
    // | 2025-06-15 | BUY_2024+2025=285.93| 568.88 | yes      |
    // | 2026-06-15 | excluded            | —      | —        |
    //
    // Expected: yearGains=568.88, totalGains=659.78
    @Test
    void multiYear_allYears_futureExcluded() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 3, 15), Money.of(new BigDecimal("5.9000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.9100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 6, 15), Money.of(new BigDecimal("5.8000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.8100"), MoneyUtil.BRL));
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025, BUY_2026), List.of(SELL_2024, SELL_2025, SELL_2026), 2025, usConverter);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("659.78"), MoneyUtil.BRL));
    }

    @Test
    void calculateCapitalGains_emptyBuysAndSells() {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(), List.of(), 2025, usConverter);

        assertThat(result.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.avgCostBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
    }

    // --- calculateDividendsBrl ---

    // | Date       | Gross | Tax  | PTAX Venda | GrossBrl | TaxBrl |
    // |------------|-------|------|------------|----------|--------|
    // | 2025-01-15 | $0.75 | $0.05| 6.0380     | 4.53     | 0.30   |
    // | 2025-06-15 | $1.20 | $0   | 5.7100     | 6.85     | 0      |
    //
    // | Result           | Value |
    // |------------------|-------|
    // | dividendGrossBrl | 11.38 |
    // | dividendTaxBrl   | 0.30  |
    static Stream<Arguments> dividendsArgs() {
        return Stream.of(
                Arguments.of(Named.of("chronological order", List.of(DIV_JAN, DIV_JUN))),
                Arguments.of(Named.of("reversed order", List.of(DIV_JUN, DIV_JAN)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dividendsArgs")
    void calculateDividendsBrl_twoDividends(List<Dividend> dividends) {
        DividendsResult result = irpfCalculator.calculateDividendsBrl(dividends, usConverter);

        assertThat(result.dividendGrossBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("11.38"), MoneyUtil.BRL));
        assertThat(result.dividendTaxBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("0.30"), MoneyUtil.BRL));
    }

    @Test
    void calculateDividendsBrl_emptyList() {
        DividendsResult result = irpfCalculator.calculateDividendsBrl(List.of(), usConverter);

        assertThat(result.dividendGrossBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.dividendTaxBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
    }

    // --- BR capital gains (BrCurrencyConverter, no PTAX) ---

    private static final CurrencyConverter brConverter = new BrCurrencyConverter();

    private static final Asset PETR4 = new Asset("PETR4", MoneyUtil.BRL);

    private static final Operation BR_BUY_JAN = Operation.builder()
            .date(LocalDate.of(2025, 1, 15)).type(OperationType.BUY).asset(PETR4)
            .price(Money.of(new BigDecimal("30.00"), MoneyUtil.BRL))
            .tax(Money.of(new BigDecimal("0.50"), MoneyUtil.BRL))
            .amount(BigDecimal.TEN).build();

    private static final Operation BR_SELL_JUN = Operation.builder()
            .date(LocalDate.of(2025, 6, 15)).type(OperationType.SELL).asset(PETR4)
            .price(Money.of(new BigDecimal("40.00"), MoneyUtil.BRL))
            .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
            .amount(new BigDecimal("5")).build();

    // | Date       | Op   | Qty | Price   | Tax    |
    // |------------|------|-----|---------|--------|
    // | 2025-01-15 | BUY  | 10  | R$30.00 | R$0.50 |
    // | 2025-06-15 | SELL | 5   | R$40.00 | R$0.00 |
    //
    // | Result          | Value  |
    // |-----------------|--------|
    // | avgCostBrl      | 30.05  |
    // | sellBrl         | 200.00 |
    // | costBrl (5)     | 150.25 |
    // | capitalGainsBrl | 49.75  |
    @Test
    void calculateBrCapitalGains_sellWithGain() {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(
                List.of(BR_BUY_JAN), List.of(BR_SELL_JUN), 2025, brConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("49.75"), MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("30.05"), MoneyUtil.BRL));
    }

    // | Date       | Op   | Qty | Price   | Tax  |
    // |------------|------|-----|---------|------|
    // | 2025-01-15 | BUY  | 10  | R$50.00 | R$0  |
    // | 2025-06-15 | SELL | 5   | R$30.00 | R$0  |
    //
    // | Result          | Value   |
    // |-----------------|---------|
    // | capitalGainsBrl | -100.00 |
    @Test
    void calculateBrCapitalGains_sellWithLoss() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15)).type(OperationType.BUY).asset(PETR4)
                .price(Money.of(new BigDecimal("50.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(BigDecimal.TEN).build();

        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 6, 15)).type(OperationType.SELL).asset(PETR4)
                .price(Money.of(new BigDecimal("30.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(new BigDecimal("5")).build();

        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(
                List.of(buy), List.of(sell), 2025, brConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("-100.00"), MoneyUtil.BRL));
    }

    @Test
    void calculateBrCapitalGains_noSells() {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(
                List.of(BR_BUY_JAN), List.of(), 2025, brConverter);

        assertThat(result.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("30.05"), MoneyUtil.BRL));
        assertThat(result.avgCostOriginal().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("30.05"), MoneyUtil.BRL));
    }

    // | Date       | Op   | Qty | Price   | Tax    |
    // |------------|------|-----|---------|--------|
    // | 2025-01-15 | BUY  | 10  | R$30.00 | R$0.50 |
    // | 2025-03-15 | BUY  | 10  | R$35.00 | R$0.00 |
    // | 2025-02-15 | SELL | 5   | R$32.00 | R$0.00 |
    // | 2025-06-15 | SELL | 5   | R$40.00 | R$0.00 |
    //
    // | Sell       | Avg at sell date          | SellBrl | CostBrl  | Gain   |
    // |------------|--------------------------|---------|----------|--------|
    // | 2025-02-15 | 300.50/10 = 30.05        | 160.00  | 150.25   | 9.75   |
    // | 2025-06-15 | (300.50+350)/20 = 32.525 | 200.00  | 162.625  | 37.375 |
    //
    // | Result          | Value |
    // |-----------------|-------|
    // | capitalGainsBrl | 47.12 |
    @Test
    void calculateBrCapitalGains_multipleBuysAndSells() {
        Operation buyMar = Operation.builder()
                .date(LocalDate.of(2025, 3, 15)).type(OperationType.BUY).asset(PETR4)
                .price(Money.of(new BigDecimal("35.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(BigDecimal.TEN).build();

        Operation sellFeb = Operation.builder()
                .date(LocalDate.of(2025, 2, 15)).type(OperationType.SELL).asset(PETR4)
                .price(Money.of(new BigDecimal("32.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(new BigDecimal("5")).build();

        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(
                List.of(BR_BUY_JAN, buyMar), List.of(sellFeb, BR_SELL_JUN), 2025, brConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("47.12"), MoneyUtil.BRL));
    }

    @Test
    void calculateBrCapitalGains_sellMoreThanHeld() {
        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 6, 15)).type(OperationType.SELL).asset(PETR4)
                .price(Money.of(new BigDecimal("40.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(new BigDecimal("11")).build();

        List<Operation> buys = List.of(BR_BUY_JAN);
        List<Operation> sells = List.of(sell);
        var calculator = irpfCalculator;
        var year = 2025;
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> calculator.calculateCapitalGains(buys, sells, year, brConverter));
    }

    // | Date       | Op   | Qty | Price   |
    // |------------|------|-----|---------|
    // | 2024-06-15 | BUY  | 10  | R$30.00 |
    // | 2024-09-15 | SELL | 3   | R$35.00 |
    // | 2025-06-15 | SELL | 5   | R$40.00 |
    //
    // | Sell       | Avg at sell date | SellBrl | CostBrl | Gain  | In year? |
    // |------------|-----------------|---------|---------|-------|----------|
    // | 2024-09-15 | 30.00           | 105.00  | 90.00   | 15.00 | no       |
    // | 2025-06-15 | 30.00           | 200.00  | 150.00  | 50.00 | yes      |
    //
    // Expected: yearGains=50.00, totalGains=65.00
    @Test
    void calculateBrCapitalGains_multiYear() {
        Operation buy2024 = Operation.builder()
                .date(LocalDate.of(2024, 6, 15)).type(OperationType.BUY).asset(PETR4)
                .price(Money.of(new BigDecimal("30.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(BigDecimal.TEN).build();

        Operation sell2024 = Operation.builder()
                .date(LocalDate.of(2024, 9, 15)).type(OperationType.SELL).asset(PETR4)
                .price(Money.of(new BigDecimal("35.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(new BigDecimal("3")).build();

        Operation sell2025 = Operation.builder()
                .date(LocalDate.of(2025, 6, 15)).type(OperationType.SELL).asset(PETR4)
                .price(Money.of(new BigDecimal("40.00"), MoneyUtil.BRL))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.BRL))
                .amount(new BigDecimal("5")).build();

        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(
                List.of(buy2024), List.of(sell2024, sell2025), 2025, brConverter);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.BRL));
        assertThat(result.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("65.00"), MoneyUtil.BRL));
    }

    // --- BR dividends (BrCurrencyConverter, no PTAX) ---

    // | Date       | Type     | Gross  | Tax   |
    // |------------|----------|--------|-------|
    // | 2025-01-15 | DIVIDEND | R$2.50 | R$0.30|
    // | 2025-06-15 | DIVIDEND | R$3.00 | R$0   |
    //
    // | Result           | Value |
    // |------------------|-------|
    // | dividendGrossBrl | 5.50  |
    // | dividendTaxBrl   | 0.30  |
    // | jcpGrossBrl      | 0     |
    // | jcpTaxBrl        | 0     |
    @Test
    void calculateBrDividends_twoDividends() {
        Dividend div1 = new Dividend(LocalDate.of(2025, 1, 15), DividendType.DIVIDEND,
                Money.of(new BigDecimal("2.50"), MoneyUtil.BRL),
                Money.of(new BigDecimal("0.30"), MoneyUtil.BRL), PETR4);
        Dividend div2 = new Dividend(LocalDate.of(2025, 6, 15), DividendType.DIVIDEND,
                Money.of(new BigDecimal("3.00"), MoneyUtil.BRL),
                Money.of(BigDecimal.ZERO, MoneyUtil.BRL), PETR4);

        DividendsResult result = irpfCalculator.calculateDividendsBrl(List.of(div1, div2), brConverter);

        assertThat(result.dividendGrossBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("5.50"), MoneyUtil.BRL));
        assertThat(result.dividendTaxBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("0.30"), MoneyUtil.BRL));
        assertThat(result.jcpGrossBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.jcpTaxBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
    }

    // | Date       | Type              | Gross  | Tax   |
    // |------------|-------------------|--------|-------|
    // | 2025-01-15 | DIVIDEND          | R$2.50 | R$0.00|
    // | 2025-03-15 | INTEREST_ON_EQUITY| R$4.00 | R$0.60|
    // | 2025-06-15 | DIVIDEND          | R$3.00 | R$0.00|
    // | 2025-06-15 | INTEREST_ON_EQUITY| R$1.50 | R$0.23|
    //
    // | Result           | Value |
    // |------------------|-------|
    // | dividendGrossBrl | 5.50  |
    // | dividendTaxBrl   | 0     |
    // | jcpGrossBrl      | 5.50  |
    // | jcpTaxBrl        | 0.83  |
    @Test
    void calculateBrDividends_mixedDividendsAndJcp() {
        Dividend div1 = new Dividend(LocalDate.of(2025, 1, 15), DividendType.DIVIDEND,
                Money.of(new BigDecimal("2.50"), MoneyUtil.BRL),
                Money.of(BigDecimal.ZERO, MoneyUtil.BRL), PETR4);
        Dividend jcp1 = new Dividend(LocalDate.of(2025, 3, 15), DividendType.INTEREST_ON_EQUITY,
                Money.of(new BigDecimal("4.00"), MoneyUtil.BRL),
                Money.of(new BigDecimal("0.60"), MoneyUtil.BRL), PETR4);
        Dividend div2 = new Dividend(LocalDate.of(2025, 6, 15), DividendType.DIVIDEND,
                Money.of(new BigDecimal("3.00"), MoneyUtil.BRL),
                Money.of(BigDecimal.ZERO, MoneyUtil.BRL), PETR4);
        Dividend jcp2 = new Dividend(LocalDate.of(2025, 6, 15), DividendType.INTEREST_ON_EQUITY,
                Money.of(new BigDecimal("1.50"), MoneyUtil.BRL),
                Money.of(new BigDecimal("0.23"), MoneyUtil.BRL), PETR4);

        DividendsResult result = irpfCalculator.calculateDividendsBrl(List.of(div1, jcp1, div2, jcp2), brConverter);

        assertThat(result.dividendGrossBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("5.50"), MoneyUtil.BRL));
        assertThat(result.dividendTaxBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.jcpGrossBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("5.50"), MoneyUtil.BRL));
        assertThat(result.jcpTaxBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("0.83"), MoneyUtil.BRL));
    }

    @Test
    void calculateBrDividends_fractionsGroupedAsUnknown() {
        Dividend div = new Dividend(LocalDate.of(2025, 1, 15), DividendType.DIVIDEND,
                Money.of(new BigDecimal("2.00"), MoneyUtil.BRL),
                Money.of(BigDecimal.ZERO, MoneyUtil.BRL), PETR4);
        Dividend frac = new Dividend(LocalDate.of(2025, 3, 15), DividendType.FRACTIONS,
                Money.of(new BigDecimal("0.50"), MoneyUtil.BRL),
                Money.of(BigDecimal.ZERO, MoneyUtil.BRL), PETR4);

        DividendsResult result = irpfCalculator.calculateDividendsBrl(List.of(div, frac), brConverter);

        assertThat(result.dividendGrossBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("2.00"), MoneyUtil.BRL));
        assertThat(result.jcpGrossBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.unknownGrossBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("0.50"), MoneyUtil.BRL));
    }

    @Test
    void calculateBrDividends_emptyList() {
        DividendsResult result = irpfCalculator.calculateDividendsBrl(List.of(), brConverter);

        assertThat(result.dividendGrossBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.dividendTaxBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.jcpGrossBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.jcpTaxBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
    }
}
