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

import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class IrpfCalculatorTest {

    @Inject
    IrpfCalculator irpfCalculator;

    @Inject
    FakePtaxService fakePtaxService;

    private static final Asset AAPL = new Asset("AAPL", MoneyUtil.USD);

    @BeforeEach
    void setUp() {
        fakePtaxService.setRate(LocalDate.of(2025, 1, 15), new BigDecimal("6.0370"), new BigDecimal("6.0380"));
        fakePtaxService.setRate(LocalDate.of(2025, 2, 15), new BigDecimal("5.8000"), new BigDecimal("5.8100"));
        fakePtaxService.setRate(LocalDate.of(2025, 3, 15), new BigDecimal("5.5000"), new BigDecimal("5.5100"));
        fakePtaxService.setRate(LocalDate.of(2025, 6, 15), new BigDecimal("5.7000"), new BigDecimal("5.7100"));
    }

    // --- calculateAverageCostBrl ---

    @Test
    void calculateAverageCostBrl_twoBuys() {
        Operation buy1 = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(50, MoneyUtil.USD))
                .tax(Money.of(new BigDecimal("0.50"), MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

        Operation buy2 = Operation.builder()
                .date(LocalDate.of(2025, 2, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(60, MoneyUtil.USD))
                .tax(Money.of(new BigDecimal("0.30"), MoneyUtil.USD))
                .amount(new BigDecimal("5"))
                .build();

        // buy1: (50 * 10 + 0.50) * 6.0370 = 500.50 * 6.0370 = 3021.5185
        // buy2: (60 *  5 + 0.30) * 5.8000 = 300.30 * 5.8000 = 1741.74
        // total = 4763.2585, amount = 15
        // avg = 4763.2585 / 15 = 317.550566...
        Money result = irpfCalculator.calculateAverageCostBrl(List.of(buy1, buy2), fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("317.55"), MoneyUtil.BRL));
    }

    @Test
    void calculateAverageCostBrl_singleBuy() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(100, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("2"))
                .build();

        // (100 * 2 + 0) * 6.0370 = 200 * 6.0370 = 1207.40
        // avg = 1207.40 / 2 = 603.70
        Money result = irpfCalculator.calculateAverageCostBrl(List.of(buy), fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("603.70"), MoneyUtil.BRL));
    }

    @Test
    void calculateAverageCostBrl_emptyList() {
        Money result = irpfCalculator.calculateAverageCostBrl(List.of(), fakePtaxService);

        assertThat(result).isEqualTo(Money.zero(MoneyUtil.BRL));
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
        Money result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
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
        Money result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("-734.50"), MoneyUtil.BRL));
    }

    @Test
    void calculateCapitalGains_noSells() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(50, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

        Money result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(), 2025, fakePtaxService);

        assertThat(result).isEqualTo(Money.zero(MoneyUtil.BRL));
    }

    @Test
    void calculateCapitalGains_multipleBuysAndSells() {
        Operation buy1 = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(50, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

        Operation buy2 = Operation.builder()
                .date(LocalDate.of(2025, 3, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(60, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("10"))
                .build();

        Operation sell1 = Operation.builder()
                .date(LocalDate.of(2025, 2, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(55, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("5"))
                .build();

        Operation sell2 = Operation.builder()
                .date(LocalDate.of(2025, 6, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(70, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("5"))
                .build();

        // Chronological: buy1(Jan15), sell1(Feb15), buy2(Mar15), sell2(Jun15)
        //
        // buy1: costBrl = 50*10 * 6.0370 = 3018.50, totalAmount=10, runningAvgCost = 301.85
        // sell1: sellBrl = 55*5 * 5.8100 = 1597.75
        //        costBrl = 301.85 * 5 = 1509.25
        //        gain1 = 1597.75 - 1509.25 = 88.50
        //        remaining: totalCostBrl = 3018.50 - 1509.25 = 1509.25, totalAmount = 5
        // buy2: costBrl = 60*10 * 5.5000 = 3300.00
        //       totalCostBrl = 1509.25 + 3300.00 = 4809.25, totalAmount = 15
        //       runningAvgCost = 4809.25 / 15 = 320.616666...
        // sell2: sellBrl = 70*5 * 5.7100 = 1998.50
        //        costBrl = 320.616666... * 5 = 1603.08333...
        //        gain2 = 1998.50 - 1603.08333... = 395.41666...
        // total = 88.50 + 395.41666... = 483.91666...
        Money result = irpfCalculator.calculateCapitalGains(List.of(buy1, buy2), List.of(sell1, sell2), 2025, fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("483.92"), MoneyUtil.BRL));
    }

    @Test
    void calculateCapitalGains_sellMoreThanHeld() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(50, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

        Operation sell = Operation.builder()
                .date(LocalDate.of(2025, 6, 15))
                .type(OperationType.SELL)
                .asset(AAPL)
                .price(Money.of(70, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(new BigDecimal("11"))
                .build();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, fakePtaxService));
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

        // Same date: buy must be processed before sell (stable sort with buys first in concat)
        // buy: costBrl = 50*10 * 6.0370 = 3018.50, runningAvg = 301.85
        // sell: sellBrl = 55*5 * 6.0380 = 1660.45, costBrl = 301.85*5 = 1509.25
        // gain = 1660.45 - 1509.25 = 151.20
        Money result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("151.20"), MoneyUtil.BRL));
    }

    @Test
    void calculateCapitalGains_sellAllShares() {
        Operation buy = Operation.builder()
                .date(LocalDate.of(2025, 1, 15))
                .type(OperationType.BUY)
                .asset(AAPL)
                .price(Money.of(50, MoneyUtil.USD))
                .tax(Money.of(BigDecimal.ZERO, MoneyUtil.USD))
                .amount(BigDecimal.TEN)
                .build();

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
        Money result = irpfCalculator.calculateCapitalGains(List.of(buy), List.of(sell), 2025, fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("978.50"), MoneyUtil.BRL));
    }

    // --- calculateDividendsBrl ---

    @Test
    void calculateDividendsBrl_twoDividends() {
        Dividend d1 = new Dividend(
                LocalDate.of(2025, 1, 15),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("0.75"), MoneyUtil.USD),
                Money.of(new BigDecimal("0.05"), MoneyUtil.USD),
                AAPL
        );

        Dividend d2 = new Dividend(
                LocalDate.of(2025, 6, 15),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("1.20"), MoneyUtil.USD),
                Money.of(BigDecimal.ZERO, MoneyUtil.USD),
                AAPL
        );

        // d1: (0.75 - 0.05) * 6.0380 = 0.70 * 6.0380 = 4.2266
        // d2: (1.20 - 0.00) * 5.7100 = 1.20 * 5.7100 = 6.852
        // total = 4.2266 + 6.852 = 11.0786
        Money result = irpfCalculator.calculateDividendsBrl(List.of(d1, d2), fakePtaxService);

        assertThat(result.with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("11.08"), MoneyUtil.BRL));
    }

    @Test
    void calculateDividendsBrl_emptyList() {
        Money result = irpfCalculator.calculateDividendsBrl(List.of(), fakePtaxService);

        assertThat(result).isEqualTo(Money.zero(MoneyUtil.BRL));
    }
}
