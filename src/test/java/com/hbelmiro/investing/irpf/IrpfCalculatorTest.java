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
        // avgCostUsd: (50*10+0)/10 = 50.00
        assertThat(result.avgCostUsd().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.USD));
    }

    // BUY_JAN: 10 shares @ $50, ptaxCompra=6.0370 → costBrl=3018.50, costUsd=500
    // BUY_MAR: 10 shares @ $60, ptaxCompra=5.5000 → costBrl=3300.00, costUsd=600
    //
    // Point-in-time avg: each sell uses avg from buys up to that sell date.
    // SELL_FEB (Feb 15): buys up to Feb 15 = BUY_JAN only → avg=3018.50/10=301.85
    //   gain = (55*5*5.81) - (301.85*5) = 1597.75 - 1509.25 = 88.50
    // SELL_JUN (Jun 15): buys up to Jun 15 = BUY_JAN + BUY_MAR → avg=6318.50/20=315.925
    //   gain = (70*5*5.71) - (315.925*5) = 1998.50 - 1579.625 = 418.875
    // totalGains = 88.50 + 418.875 = 507.375 → 507.38
    //
    // avgCostBrl at endOfYear (for Bens e Direitos) = all buys = 315.92
    // avgCostUsd at endOfYear = (500+600)/20 = 55.00
    static Stream<Arguments> multipleBuysAndSellsArgs() {
        return Stream.of(
                Arguments.of(Named.of("chronological order", List.of(BUY_JAN, BUY_MAR)), List.of(SELL_FEB, SELL_JUN)),
                Arguments.of(Named.of("reversed order", List.of(BUY_MAR, BUY_JAN)), List.of(SELL_JUN, SELL_FEB))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multipleBuysAndSellsArgs")
    void calculateCapitalGains_multipleBuysAndSells(List<Operation> buys, List<Operation> sells) {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(buys, sells, 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("507.38"), MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("315.92"), MoneyUtil.BRL));
        assertThat(result.avgCostUsd().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("55.00"), MoneyUtil.USD));
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

        // avgCostBrl = 50*10 * 6.0370 / 10 = 301.85 (buy-only, unchanged by sell)
        // sell all: sellBrl = 70*10 * 5.7100 = 3997.00, costBrl = 301.85*10 = 3018.50
        // gain = 3997.00 - 3018.50 = 978.50
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(BUY_JAN), List.of(sell), 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("978.50"), MoneyUtil.BRL));
        assertThat(result.avgCostBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("301.85"), MoneyUtil.BRL));
    }

    // --- Multi-year capital gains matrix (target year=2025) ---
    // Operations used:
    //   BUY_2024:  2024-06-15, 10 @ $50, ptaxCompra=5.4000 → costBrl=2700.00
    //   SELL_2024: 2024-09-15, 3 @ $55,  ptaxVenda=5.4600  → sellBrl=900.90, cost=3*avgCost
    //   BUY_2025:  2025-01-15, 10 @ $50, ptaxCompra=6.0370 → costBrl=3018.50 (= BUY_JAN)
    //   SELL_2025: 2025-06-15, 5 @ $70,  ptaxVenda=5.7100  → sellBrl=1998.50, cost=5*avgCost (= SELL_JUN)
    //   BUY_2026:  2026-03-15, 8 @ $65,  ptaxCompra=5.9000 → costBrl=3068.00
    //   SELL_2026: 2026-06-15, 4 @ $75,  ptaxVenda=5.8000  → sellBrl=1740.00, cost=4*avgCost

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

    // #1: only B in 2025
    @Test
    void multiYear_onlyBuy2025() {
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2025), List.of(), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.avgCostUsd().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.USD));
    }

    // #2: B+S in 2025
    @Test
    void multiYear_buyAndSell2025() {
        // avg = 3018.50/10 = 301.85 BRL, 50 USD
        // sell: 1998.50 - 301.85*5 = 1998.50 - 1509.25 = 489.25
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2025), List.of(SELL_2025), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("489.25"), MoneyUtil.BRL));
        assertThat(r.capitalGainsBrl()).isEqualTo(r.totalCapitalGainsBrl());
    }

    // #3: only S in 2025 → error (no buys)
    @Test
    void multiYear_onlySell2025_nobuys() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> irpfCalculator.calculateCapitalGains(
                        List.of(), List.of(SELL_2025), 2025, fakePtaxService));
    }

    // #4: B(24) + B(25)
    @Test
    void multiYear_buy2024_buy2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        // avg = (2700+3018.50)/20 = 5718.50/20 = 285.925 BRL, (500+500)/20 = 50 USD
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025), List.of(), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.avgCostUsd().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.USD));
    }

    // #5: B(24) + S(25)
    @Test
    void multiYear_buy2024_sell2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        // avg = 2700/10 = 270 BRL
        // sell 2025: 1998.50 - 270*5 = 1998.50 - 1350 = 648.50
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024), List.of(SELL_2025), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("648.50"), MoneyUtil.BRL));
        assertThat(r.capitalGainsBrl()).isEqualTo(r.totalCapitalGainsBrl());
    }

    // #6: B(24) + B+S(25)
    @Test
    void multiYear_buy2024_buyAndSell2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        // avg = (2700+3018.50)/20 = 285.925 BRL
        // sell 2025: 1998.50 - 285.925*5 = 1998.50 - 1429.625 = 568.875 → 568.88
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025), List.of(SELL_2025), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.capitalGainsBrl()).isEqualTo(r.totalCapitalGainsBrl());
    }

    // #7: B+S(24) + B(25)
    @Test
    void multiYear_buyAndSell2024_buy2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));
        // sell 2024: avg at sell date = BUY_2024 only = 2700/10 = 270
        //   gain = 55*3*5.46 - 270*3 = 900.90 - 810 = 90.90 → NOT in year
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025), List.of(SELL_2024), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("90.90"), MoneyUtil.BRL));
    }

    // #8: B+S(24) + S(25)
    @Test
    void multiYear_buyAndSell2024_sell2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));
        // avg = 2700/10 = 270 BRL
        // sell 2024: 900.90 - 270*3=810 = 90.90 → NOT in year
        // sell 2025: 1998.50 - 270*5=1350 = 648.50 → in year
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024), List.of(SELL_2024, SELL_2025), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("648.50"), MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("739.40"), MoneyUtil.BRL));
    }

    // #9: B+S(24) + B+S(25)
    @Test
    void multiYear_buyAndSell2024_buyAndSell2025() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));
        // sell 2024: avg at date = BUY_2024 only = 270 → gain = 900.90 - 810 = 90.90 → NOT in year
        // sell 2025: avg at date = BUY_2024+BUY_2025 = 285.925 → gain = 1998.50 - 1429.625 = 568.875 → in year
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025), List.of(SELL_2024, SELL_2025), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("659.78"), MoneyUtil.BRL));
    }

    // #10: B(24) + B+S(25) + B(26) — calculator must exclude future buys from avg cost
    @Test
    void multiYear_buy2024_buyAndSell2025_buy2026excluded() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 3, 15), Money.of(new BigDecimal("5.9000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.9100"), MoneyUtil.BRL));
        // avg = (2700+3018.50)/20 = 285.925 BRL (BUY_2026 excluded from avg)
        // sell 2025: 568.88
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025, BUY_2026), List.of(SELL_2025), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.avgCostUsd().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("50.00"), MoneyUtil.USD));
    }

    // #11: B(24) + B+S(25) + B+S(26) — calculator must exclude future buys and sells
    @Test
    void multiYear_buy2024_buyAndSell2025_buyAndSell2026excluded() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 3, 15), Money.of(new BigDecimal("5.9000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.9100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 6, 15), Money.of(new BigDecimal("5.8000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.8100"), MoneyUtil.BRL));
        // BUY_2026 excluded from avg, SELL_2026 excluded from gains
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025, BUY_2026), List.of(SELL_2025, SELL_2026), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.capitalGainsBrl()).isEqualTo(r.totalCapitalGainsBrl());
    }

    // #12: B+S(24) + B+S(25) + B+S(26) — future ops excluded
    @Test
    void multiYear_allYears_futureExcluded() {
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 3, 15), Money.of(new BigDecimal("5.9000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.9100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2026, 6, 15), Money.of(new BigDecimal("5.8000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.8100"), MoneyUtil.BRL));
        // sell 2024: avg at date = BUY_2024 only = 270 → gain = 90.90
        // sell 2025: avg at date = BUY_2024+BUY_2025 = 285.925 → gain = 568.88
        // SELL_2026 excluded, BUY_2026 excluded
        CapitalGainsResult r = irpfCalculator.calculateCapitalGains(
                List.of(BUY_2024, BUY_2025, BUY_2026), List.of(SELL_2024, SELL_2025, SELL_2026), 2025, fakePtaxService);
        assertThat(r.capitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("568.88"), MoneyUtil.BRL));
        assertThat(r.totalCapitalGainsBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("659.78"), MoneyUtil.BRL));
    }

    @Test
    void calculateCapitalGains_emptyBuysAndSells() {
        CapitalGainsResult result = irpfCalculator.calculateCapitalGains(List.of(), List.of(), 2025, fakePtaxService);

        assertThat(result.capitalGainsBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.avgCostBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
    }

    // --- calculateDividendsBrl ---

    // d1: gross=0.75*6.0380=4.5285, tax=0.05*6.0380=0.3019
    // d2: gross=1.20*5.7100=6.852, tax=0.00*5.7100=0
    // totalGross = 4.5285 + 6.852 = 11.3805
    // totalTax = 0.3019 + 0 = 0.3019
    static Stream<Arguments> dividendsArgs() {
        return Stream.of(
                Arguments.of(Named.of("chronological order", List.of(DIV_JAN, DIV_JUN))),
                Arguments.of(Named.of("reversed order", List.of(DIV_JUN, DIV_JAN)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dividendsArgs")
    void calculateDividendsBrl_twoDividends(List<Dividend> dividends) {
        DividendsResult result = irpfCalculator.calculateDividendsBrl(dividends, fakePtaxService);

        assertThat(result.grossBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("11.38"), MoneyUtil.BRL));
        assertThat(result.taxBrl().with(Monetary.getDefaultRounding()))
                .isEqualTo(Money.of(new BigDecimal("0.30"), MoneyUtil.BRL));
    }

    @Test
    void calculateDividendsBrl_emptyList() {
        DividendsResult result = irpfCalculator.calculateDividendsBrl(List.of(), fakePtaxService);

        assertThat(result.grossBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
        assertThat(result.taxBrl()).isEqualTo(Money.zero(MoneyUtil.BRL));
    }
}
