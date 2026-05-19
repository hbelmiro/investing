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
        fakePtaxService.setRate(LocalDate.of(2025, 1, 15), new BigDecimal("6.0370"), new BigDecimal("6.0380"));
        fakePtaxService.setRate(LocalDate.of(2025, 2, 15), new BigDecimal("5.8000"), new BigDecimal("5.8100"));
        fakePtaxService.setRate(LocalDate.of(2025, 3, 15), new BigDecimal("5.5000"), new BigDecimal("5.5100"));
        fakePtaxService.setRate(LocalDate.of(2025, 6, 15), new BigDecimal("5.7000"), new BigDecimal("5.7100"));
    }

    // --- calculateCapitalGains ---

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

        // buy: costBrl = (50 * 10 + 0.50) * 6.0370 = 500.50 * 6.0370 = 3021.5185
        // runningAvgCostBrl = 3021.5185 / 10 = 302.15185
        // sell: sellBrl = 70 * 5 * 5.7100 = 1998.50
        //       costBrl = 302.15185 * 5 = 1510.75925
        //       gain = 1998.50 - 1510.75925 = 487.74075
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("487.74"), MoneyUtil.BRL));
    }

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

        // buy: costBrl = (100 * 10) * 6.0370 = 6037.00
        // runningAvgCostBrl = 6037.00 / 10 = 603.70
        // sell: sellBrl = 80 * 5 * 5.7100 = 2284.00
        //       costBrl = 603.70 * 5 = 3018.50
        //       gain = 2284.00 - 3018.50 = -734.50
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("-734.50"), MoneyUtil.BRL));
    }

    @Test
    void calculateCapitalGains_noSells() {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(BUY_JAN), List.of(), 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("301.85"), MoneyUtil.BRL));
    }

    // Chronological: BUY_JAN, SELL_FEB, BUY_MAR, SELL_JUN
    // buy1: costBrl=3018.50, amount=10, avg=301.85
    // sell1: depletes 5 → totalCost=1509.25, amount=5
    // buy2: costBrl=3300.00 → totalCost=4809.25, amount=15, avg=320.62
    // sell2: depletes 5 → totalCost=3206.17, amount=10, avg=320.62
    // gains = 88.50 + 395.42 = 483.92
    static Stream<Arguments> multipleBuysAndSellsArgs() {
        return Stream.of(
                Arguments.of("chronological order", List.of(BUY_JAN, BUY_MAR), List.of(SELL_FEB, SELL_JUN)),
                Arguments.of("reversed order", List.of(BUY_MAR, BUY_JAN), List.of(SELL_JUN, SELL_FEB))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multipleBuysAndSellsArgs")
    void calculateCapitalGains_multipleBuysAndSells(String name, List<Operation> buys, List<Operation> sells) {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(buys, sells, 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("483.92"), MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("320.62"), MoneyUtil.BRL));
    }

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

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> irpfCalculator.calculateCapitalGains(List.of(BUY_JAN), List.of(sell), 2025, fakePtaxService));
    }

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

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> irpfCalculator.calculateCapitalGains(List.of(), List.of(sell), 2025, fakePtaxService));
    }

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

        // buy: costBrl = 50*10 * 6.0370 = 3018.50, runningAvg = 301.85
        // sell: sellBrl = 55*5 * 6.0380 = 1660.45, costBrl = 301.85*5 = 1509.25
        // gain = 1660.45 - 1509.25 = 151.20
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("151.20"), MoneyUtil.BRL));
    }

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

        // buy: costBrl = 50*10 * 6.0370 = 3018.50, runningAvg = 301.85
        // sell all: sellBrl = 70*10 * 5.7100 = 3997.00, costBrl = 301.85*10 = 3018.50
        // gain = 3997.00 - 3018.50 = 978.50
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(BUY_JAN), List.of(sell), 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("978.50"), MoneyUtil.BRL));
        assertThat(result.avgCostBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
    }

    @Test
    void calculateCapitalGains_emptyBuysAndSells() {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(), List.of(), 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.avgCostBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
    }

    // --- calculateDividendsBrl ---

    // d1: (0.75 - 0.05) * 6.0380 = 0.70 * 6.0380 = 4.2266
    // d2: (1.20 - 0.00) * 5.7100 = 1.20 * 5.7100 = 6.852
    // total = 4.2266 + 6.852 = 11.0786
    static Stream<Arguments> dividendsArgs() {
        return Stream.of(
                Arguments.of("chronological order", List.of(DIV_JAN, DIV_JUN)),
                Arguments.of("reversed order", List.of(DIV_JUN, DIV_JAN))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dividendsArgs")
    void calculateDividendsBrl_twoDividends(String name, List<Dividend> dividends) {
        Money result = irpfCalculator.calculateDividendsBrl(dividends, fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("11.08"), MoneyUtil.BRL));
    }

    @Test
    void calculateDividendsBrl_emptyList() {
        Money result = irpfCalculator.calculateDividendsBrl(List.of(), fakePtaxService);

        assertThat(result).isEqualTo(Money.zero(MoneyUtil.BRL));
    }
}
