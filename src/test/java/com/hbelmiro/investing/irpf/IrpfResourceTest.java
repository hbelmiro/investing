package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
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
class IrpfResourceTest {

    @Inject
    IrpfResource irpfResource;

    @Inject
    CsvGoogleSheetsClient csvGoogleSheetsClient;

    @Inject
    FakePtaxService fakePtaxService;

    @BeforeEach
    void setUp() {
        csvGoogleSheetsClient.setCsv("Compras Ações US", "/csv/IrpfResource/us_buys.csv");
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells.csv");
        csvGoogleSheetsClient.setCsv("Compras Renda Fixa US", "/csv/IrpfResource/empty.csv");
        csvGoogleSheetsClient.setCsv("Vendas Renda Fixa US", "/csv/IrpfResource/empty.csv");
        csvGoogleSheetsClient.setCsv("Compras REITS", "/csv/IrpfResource/empty.csv");
        csvGoogleSheetsClient.setCsv("Vendas REITS", "/csv/IrpfResource/empty.csv");
        csvGoogleSheetsClient.setCsv("Proventos US", "/csv/IrpfResource/us_dividends.csv");

        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 1, 15), Money.of(new BigDecimal("6.0370"), MoneyUtil.BRL), Money.of(new BigDecimal("6.0380"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 3, 15), Money.of(new BigDecimal("5.5000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.5100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 6, 15), Money.of(new BigDecimal("5.7000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.7100"), MoneyUtil.BRL));
    }

    @Test
    void getUsStocksIrpf() {
        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).hasSize(2);

        // AAPL: buys in 2024 + 2025, sell in 2025, dividend in 2025
        // avgCostBrl: (500.50*5.4000 + 300.30*5.5000) / 15 = (2702.70 + 1651.65) / 15 = 290.29
        // avgCostUsd: (500.50 + 300.30) / 15 = 800.80 / 15 = 53.39 (sell depletes but avg stays)
        // capitalGains: sellBrl(70*3*5.7100=1199.10) - costBrl(290.29*3=870.87) = 328.23
        // quantity: 15 bought - 3 sold = 12
        // totalCostBrl: 290.29 * 12 = 3483.48
        // totalCostUsd: 53.39 * 12 = 640.68 (sell depletes 3 shares at avg cost)
        // dividendsBrl: (0.75-0.10)*5.7100 = 0.65*5.7100 = 3.71
        IrpfAssetData aapl = result.get(0);
        assertThat(aapl.symbol()).isEqualTo("AAPL");
        assertThat(aapl.quantity()).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(aapl.avgCostBrl()).isEqualTo(brl("290.29"));
        assertThat(aapl.totalCostBrl()).isEqualTo(brl("3483.48"));
        assertThat(aapl.avgCostUsd()).isEqualTo(usd("53.39"));
        assertThat(aapl.totalCostUsd()).isEqualTo(usd("640.68"));
        assertThat(aapl.ptaxRate()).isNotNull();
        assertThat(aapl.capitalGainsBrl()).isEqualTo(brl("328.23"));
        assertThat(aapl.totalDividendsBrl()).isEqualTo(brl("3.71"));

        // MSFT: buy in 2025, no sells, dividend in 2025
        // avgCostBrl: (320.20*6.0370) / 8 = 1933.0474 / 8 = 241.63
        // capitalGains: 0 (no sells)
        // quantity: 8
        // totalCostBrl: 241.63 * 8 = 1933.04
        // dividendsBrl: 1.20*5.5100 = 6.61
        IrpfAssetData msft = result.get(1);
        assertThat(msft.symbol()).isEqualTo("MSFT");
        assertThat(msft.quantity()).isEqualByComparingTo(new BigDecimal("8"));
        assertThat(msft.avgCostBrl()).isEqualTo(brl("241.63"));
        assertThat(msft.totalCostBrl()).isEqualTo(brl("1933.04"));
        assertThat(msft.capitalGainsBrl()).isEqualTo(brl("0"));
        assertThat(msft.totalDividendsBrl()).isEqualTo(brl("6.61"));
    }

    @Test
    void getUsStocksIrpf_filtersOutDividendsFromOtherYears() {
        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData aapl = result.stream()
                .filter(d -> d.symbol().equals("AAPL"))
                .findFirst()
                .orElseThrow();

        // Only the 2025 dividend should be included, not the 2024 one
        // 2024 dividend: (0.50-0.05)*5.4100 = 2.4345 — NOT included
        // 2025 dividend: (0.75-0.10)*5.7100 = 3.7115 → 3.71
        assertThat(aapl.totalDividendsBrl()).isEqualTo(brl("3.71"));
    }

    @Test
    void getUsStocksIrpf_includesHistoricalBuysForCostBasis() {
        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData aapl = result.stream()
                .filter(d -> d.symbol().equals("AAPL"))
                .findFirst()
                .orElseThrow();

        // avgCostBrl includes the 2024 buy (historical cost basis)
        // If it only included the 2025 buy: avgCostBrl would be 300.30*5.5000/5 = 330.33
        // With both buys: 290.29
        assertThat(aapl.avgCostBrl()).isEqualTo(brl("290.29"));
    }

    @Test
    void getUsStocksIrpf_excludesFutureBuys() {
        csvGoogleSheetsClient.setCsv("Compras Ações US", "/csv/IrpfResource/us_buys_with_future.csv");

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(IrpfAssetData::symbol)
                .containsExactly("AAPL", "MSFT")
                .doesNotContain("GOOG");
    }

    @Test
    void getUsStocksIrpf_throwsWhenSellSymbolHasNoBuys() {
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_unknown_symbol.csv");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> irpfResource.getUsStocksIrpf(2025));
    }

    @Test
    void getUsStocksIrpf_previousYearSellsReduceQuantity() {
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_with_prior_year.csv");
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData aapl = result.stream()
                .filter(d -> d.symbol().equals("AAPL"))
                .findFirst()
                .orElseThrow();

        // AAPL: 15 bought - 2 sold(2024) - 3 sold(2025) = 10
        assertThat(aapl.quantity()).isEqualByComparingTo(new BigDecimal("10"));

        // avgCostBrl = (2702.70 + 1651.65) / 15 = 290.29 (buy-only, sells don't change avg)
        // 2025 sell: gain = (70*3*5.71) - (290.29*3) = 1199.10 - 870.87 = 328.23
        assertThat(aapl.capitalGainsBrl()).isEqualTo(brl("328.23"));

        assertThat(aapl.avgCostBrl()).isEqualTo(brl("290.29"));
        assertThat(aapl.totalCostBrl()).isEqualTo(brl("2902.90"));
    }

    @Test
    void getUsStocksIrpf_excludesFullyLiquidatedAssetWithNoYearActivity() {
        csvGoogleSheetsClient.setCsv("Compras Ações US", "/csv/IrpfResource/us_buys_with_liquidated.csv");
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_with_liquidation.csv");
        fakePtaxService.setRate(LocalDate.of(2024, 3, 15), Money.of(new BigDecimal("5.0000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.0100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        // TSLA: bought 10 in 2024, sold 10 in 2024, no 2025 activity → excluded
        assertThat(result).extracting(IrpfAssetData::symbol)
                .containsExactly("AAPL", "MSFT")
                .doesNotContain("TSLA");
    }

    @Test
    void getUsStocksIrpf_includesReits() {
        csvGoogleSheetsClient.setCsv("Compras REITS", "/csv/IrpfResource/us_reits_buys.csv");
        csvGoogleSheetsClient.setCsv("Proventos US", "/csv/IrpfResource/us_dividends_reits.csv");

        fakePtaxService.setRate(LocalDate.of(2025, 2, 15), Money.of(new BigDecimal("5.8000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.8100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 4, 15), Money.of(new BigDecimal("5.6000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.6100"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        // VNQ: buy 20 @ $80 + $0.40 tax, ptaxCompra=5.8000 → costBrl=(80*20+0.40)*5.80=9282.32, avg=464.12
        // dividend: (2.00-0.30)*5.6100 = 1.70*5.6100 = 9.54
        IrpfAssetData vnq = result.stream().filter(d -> d.symbol().equals("VNQ")).findFirst().orElseThrow();
        assertThat(vnq.quantity()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(vnq.totalDividendsBrl()).isEqualTo(brl("9.54"));
    }

    @Test
    void getUsStocksIrpf_includesFixedIncome() {
        csvGoogleSheetsClient.setCsv("Compras Renda Fixa US", "/csv/IrpfResource/us_fixed_income_buys.csv");
        csvGoogleSheetsClient.setCsv("Proventos US", "/csv/IrpfResource/us_dividends_fixed_income.csv");

        fakePtaxService.setRate(LocalDate.of(2025, 1, 10), Money.of(new BigDecimal("6.0000"), MoneyUtil.BRL), Money.of(new BigDecimal("6.0100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 5, 15), Money.of(new BigDecimal("5.5000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.5100"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        // TFLO: buy 50 @ $50 + $0.25 tax, ptaxCompra=6.0000 → costBrl=(50*50+0.25)*6.00=15001.50, avg=300.03
        // dividend: (0.50-0.00)*5.5100 = 2.76
        IrpfAssetData tflo = result.stream().filter(d -> d.symbol().equals("TFLO")).findFirst().orElseThrow();
        assertThat(tflo.quantity()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(tflo.totalDividendsBrl()).isEqualTo(brl("2.76"));
    }

    @Test
    void getUsStocksIrpf_filtersDividendsWithQuestionMarkSymbol() {
        csvGoogleSheetsClient.setCsv("Compras REITS", "/csv/IrpfResource/us_reits_buys.csv");
        csvGoogleSheetsClient.setCsv("Compras Renda Fixa US", "/csv/IrpfResource/us_fixed_income_buys.csv");
        csvGoogleSheetsClient.setCsv("Proventos US", "/csv/IrpfResource/us_dividends_multi_type.csv");

        fakePtaxService.setRate(LocalDate.of(2025, 1, 10), Money.of(new BigDecimal("6.0000"), MoneyUtil.BRL), Money.of(new BigDecimal("6.0100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 2, 15), Money.of(new BigDecimal("5.8000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.8100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 4, 15), Money.of(new BigDecimal("5.6000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.6100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 5, 15), Money.of(new BigDecimal("5.5000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.5100"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).extracting(IrpfAssetData::symbol)
                .doesNotContain("?");
    }

    @Test
    void getAvailableYears_returnsYearsFromEarliestBuyToCurrentYear() {
        List<Integer> years = irpfResource.getAvailableYears();

        assertThat(years.getFirst()).isEqualTo(2024);
        assertThat(years.getLast()).isEqualTo(LocalDate.now().getYear());
        assertThat(years).isSorted();
        for (int i = 1; i < years.size(); i++) {
            assertThat(years.get(i)).isEqualTo(years.get(i - 1) + 1);
        }
    }

    @Test
    void getUsStocksIrpf_excludesNearZeroQuantityFromFractionalLiquidation() {
        csvGoogleSheetsClient.setCsv("Compras Ações US", "/csv/IrpfResource/us_buys_fractional_liquidated.csv");
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_fractional_liquidation.csv");
        fakePtaxService.setRate(LocalDate.of(2024, 3, 15), Money.of(new BigDecimal("5.0000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.0100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).extracting(IrpfAssetData::symbol)
                .containsExactly("AAPL", "MSFT")
                .doesNotContain("ATVI");
    }

    @Test
    void getUsStocksIrpf_filtersMalformedSymbolsFromSellsAndDividends() {
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_with_placeholder.csv");
        csvGoogleSheetsClient.setCsv("Proventos US", "/csv/IrpfResource/us_dividends_malformed_symbols.csv");

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).extracting(IrpfAssetData::symbol)
                .doesNotContain("-", "?", "", "Estorno Impostos sobre Dividendos");
        assertThat(result).extracting(IrpfAssetData::symbol)
                .contains("AAPL", "MSFT");
    }

    @Test
    void getUsStocksIrpf_returnsErrorFieldForAssetWithCalculationFailure() {
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_oversell.csv");

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        // AAPL succeeds (sell 3 of 15 is valid)
        IrpfAssetData aapl = result.stream().filter(d -> d.symbol().equals("AAPL")).findFirst().orElseThrow();
        assertThat(aapl.error()).isNull();
        assertThat(aapl.avgCostBrl()).isNotNull();

        // MSFT fails (sell 99 but only 8 bought)
        IrpfAssetData msft = result.stream().filter(d -> d.symbol().equals("MSFT")).findFirst().orElseThrow();
        assertThat(msft.error()).isNotNull();
        assertThat(msft.error()).contains("exceeds total bought");
    }

    private static Money brl(String value) {
        return Money.of(new BigDecimal(value), MoneyUtil.BRL).with(Monetary.getDefaultRounding());
    }

    private static Money usd(String value) {
        return Money.of(new BigDecimal(value), MoneyUtil.USD).with(Monetary.getDefaultRounding());
    }
}
