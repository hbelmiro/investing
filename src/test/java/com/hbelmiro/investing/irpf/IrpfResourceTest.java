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

    // Default CSV pages and PTAX rates used by most tests:
    //
    // | Page             | CSV                              |
    // |------------------|----------------------------------|
    // | Compras Ações US | us_buys.csv (AAPL 10+5, MSFT 8) |
    // | Vendas Ações US  | us_sells.csv (AAPL 3)            |
    // | Proventos US     | us_dividends.csv                 |
    // | (others)         | empty.csv                        |
    //
    // | Date       | PTAX Compra | PTAX Venda |
    // |------------|-------------|------------|
    // | 2024-06-15 | 5.4000      | 5.4100     |
    // | 2025-01-15 | 6.0370      | 6.0380     |
    // | 2025-03-15 | 5.5000      | 5.5100     |
    // | 2025-06-15 | 5.7000      | 5.7100     |
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

    // AAPL operations (from us_buys.csv + us_sells.csv + us_dividends.csv):
    // | Date       | Op   | Qty | Price  | Tax   | PTAX C | PTAX V |
    // |------------|------|-----|--------|-------|--------|--------|
    // | 2024-06-15 | BUY  | 10  | $50.05 | $0.50 | 5.4000 |        |
    // | 2025-03-15 | BUY  | 5   | $60.06 | $0.30 | 5.5000 |        |
    // | 2025-06-15 | SELL | 3   | $70.00 | $0.35 |        | 5.7100 |
    // | 2025-06-15 | DIV  |     | $0.75  | $0.10 |        | 5.7100 |
    //
    // | AAPL Result         | Value   |
    // |---------------------|---------|
    // | quantity            | 12      |
    // | avgCostBrl          | 290.29  |
    // | totalCostBrl        | 3483.48 |
    // | avgCostUsd          | 53.39   |
    // | totalCostUsd        | 640.68  |
    // | capitalGainsBrl     | 328.23  |
    // | dividendsGrossBrl   | 4.28    |
    // | dividendsTaxBrl     | 0.57    |
    //
    // MSFT operations:
    // | Date       | Op  | Qty | Price  | Tax   | PTAX C | PTAX V |
    // |------------|-----|-----|--------|-------|--------|--------|
    // | 2025-01-15 | BUY | 8   | $40.03 | $0.20 | 6.0370 |        |
    // | 2025-03-15 | DIV |     | $1.20  | $0    |        | 5.5100 |
    //
    // | MSFT Result         | Value   |
    // |---------------------|---------|
    // | quantity            | 8       |
    // | avgCostBrl          | 241.63  |
    // | totalCostBrl        | 1933.04 |
    // | capitalGainsBrl     | 0       |
    // | dividendsGrossBrl   | 6.61    |
    // | dividendsTaxBrl     | 0       |
    @Test
    void getUsStocksIrpf() {
        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).hasSize(2);

        IrpfAssetData aapl = result.get(0);
        assertThat(aapl.symbol()).isEqualTo("AAPL");
        assertThat(aapl.quantity()).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(aapl.avgCostBrl()).isEqualTo(brl("290.29"));
        assertThat(aapl.totalCostBrl()).isEqualTo(brl("3483.48"));
        assertThat(aapl.avgCostUsd()).isEqualTo(usd("53.39"));
        assertThat(aapl.totalCostUsd()).isEqualTo(usd("640.68"));
        assertThat(aapl.ptaxRate()).isNotNull();
        assertThat(aapl.capitalGainsBrl()).isEqualTo(brl("328.23"));
        assertThat(aapl.dividendsGrossBrl()).isEqualTo(brl("4.28"));
        assertThat(aapl.dividendsTaxBrl()).isEqualTo(brl("0.57"));

        IrpfAssetData msft = result.get(1);
        assertThat(msft.symbol()).isEqualTo("MSFT");
        assertThat(msft.quantity()).isEqualByComparingTo(new BigDecimal("8"));
        assertThat(msft.avgCostBrl()).isEqualTo(brl("241.63"));
        assertThat(msft.totalCostBrl()).isEqualTo(brl("1933.04"));
        assertThat(msft.capitalGainsBrl()).isEqualTo(brl("0"));
        assertThat(msft.dividendsGrossBrl()).isEqualTo(brl("6.61"));
        assertThat(msft.dividendsTaxBrl()).isEqualTo(brl("0"));
    }

    // | Year | Dividend | Gross | Tax  | Included? |
    // |------|----------|-------|------|-----------|
    // | 2024 | AAPL     | $0.50 | $0.05| no        |
    // | 2025 | AAPL     | $0.75 | $0.10| yes       |
    //
    // Expected: only 2025 dividend → grossBrl=4.28, taxBrl=0.57
    @Test
    void getUsStocksIrpf_filtersOutDividendsFromOtherYears() {
        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData aapl = result.stream()
                .filter(d -> d.symbol().equals("AAPL"))
                .findFirst()
                .orElseThrow();

        assertThat(aapl.dividendsGrossBrl()).isEqualTo(brl("4.28"));
        assertThat(aapl.dividendsTaxBrl()).isEqualTo(brl("0.57"));
    }

    // | Date       | Op  | Qty | Price  | PTAX C |
    // |------------|-----|-----|--------|--------|
    // | 2024-06-15 | BUY | 10  | $50.05 | 5.4000 |
    // | 2025-03-15 | BUY | 5   | $60.06 | 5.5000 |
    //
    // Expected: avgCostBrl includes 2024 buy → 290.29 (not 330.33 if 2025 only)
    @Test
    void getUsStocksIrpf_includesHistoricalBuysForCostBasis() {
        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData aapl = result.stream()
                .filter(d -> d.symbol().equals("AAPL"))
                .findFirst()
                .orElseThrow();

        assertThat(aapl.avgCostBrl()).isEqualTo(brl("290.29"));
    }

    // us_buys_with_future.csv adds GOOG buy in 2026
    // Expected: GOOG excluded from 2025 results
    @Test
    void getUsStocksIrpf_excludesFutureBuys() {
        csvGoogleSheetsClient.setCsv("Compras Ações US", "/csv/IrpfResource/us_buys_with_future.csv");

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(IrpfAssetData::symbol)
                .containsExactly("AAPL", "MSFT")
                .doesNotContain("GOOG");
    }

    // us_sells_unknown_symbol.csv has GOOG sell with no buy history
    // Expected: IllegalStateException
    @Test
    void getUsStocksIrpf_throwsWhenSellSymbolHasNoBuys() {
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_unknown_symbol.csv");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> irpfResource.getUsStocksIrpf(2025));
    }

    // us_sells_with_prior_year.csv: AAPL sell 2 in 2024, sell 3 in 2025
    //
    // | Date       | Op   | Qty | Price  | PTAX C | PTAX V |
    // |------------|------|-----|--------|--------|--------|
    // | 2024-06-15 | BUY  | 10  | $50.05 | 5.4000 |        |
    // | 2024-09-15 | SELL | 2   | ...    |        | 5.4600 |
    // | 2025-03-15 | BUY  | 5   | $60.06 | 5.5000 |        |
    // | 2025-06-15 | SELL | 3   | $70.00 |        | 5.7100 |
    //
    // | Result          | Value   |
    // |-----------------|---------|
    // | quantity        | 10      |
    // | avgCostBrl      | 290.29  |
    // | totalCostBrl    | 2902.90 |
    // | capitalGainsBrl | 328.23  |
    @Test
    void getUsStocksIrpf_previousYearSellsReduceQuantity() {
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_with_prior_year.csv");
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), Money.of(new BigDecimal("5.4500"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4600"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData aapl = result.stream()
                .filter(d -> d.symbol().equals("AAPL"))
                .findFirst()
                .orElseThrow();

        assertThat(aapl.quantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(aapl.capitalGainsBrl()).isEqualTo(brl("328.23"));
        assertThat(aapl.avgCostBrl()).isEqualTo(brl("290.29"));
        assertThat(aapl.totalCostBrl()).isEqualTo(brl("2902.90"));
    }

    // TSLA: bought 10 in 2024, sold 10 in 2024, no 2025 activity
    // Expected: excluded from results
    @Test
    void getUsStocksIrpf_excludesFullyLiquidatedAssetWithNoYearActivity() {
        csvGoogleSheetsClient.setCsv("Compras Ações US", "/csv/IrpfResource/us_buys_with_liquidated.csv");
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_with_liquidation.csv");
        fakePtaxService.setRate(LocalDate.of(2024, 3, 15), Money.of(new BigDecimal("5.0000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.0100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), Money.of(new BigDecimal("5.4000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.4100"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).extracting(IrpfAssetData::symbol)
                .containsExactly("AAPL", "MSFT")
                .doesNotContain("TSLA");
    }

    // VNQ (REIT): read from "Compras REITS" page
    //
    // | Date       | Op  | Qty | Price | Tax   | PTAX C | PTAX V |
    // |------------|-----|-----|-------|-------|--------|--------|
    // | 2025-02-15 | BUY | 20  | $80   | $0.40 | 5.8000 |        |
    // | 2025-04-15 | DIV |     | $2.00 | $0.30 |        | 5.6100 |
    //
    // | Result            | Value |
    // |-------------------|-------|
    // | quantity          | 20    |
    // | dividendsGrossBrl | 11.22 |
    // | dividendsTaxBrl   | 1.68  |
    @Test
    void getUsStocksIrpf_includesReits() {
        csvGoogleSheetsClient.setCsv("Compras REITS", "/csv/IrpfResource/us_reits_buys.csv");
        csvGoogleSheetsClient.setCsv("Proventos US", "/csv/IrpfResource/us_dividends_reits.csv");

        fakePtaxService.setRate(LocalDate.of(2025, 2, 15), Money.of(new BigDecimal("5.8000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.8100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 4, 15), Money.of(new BigDecimal("5.6000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.6100"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData vnq = result.stream().filter(d -> d.symbol().equals("VNQ")).findFirst().orElseThrow();
        assertThat(vnq.quantity()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(vnq.dividendsGrossBrl()).isEqualTo(brl("11.22"));
        assertThat(vnq.dividendsTaxBrl()).isEqualTo(brl("1.68"));
    }

    // TFLO (fixed income): read from "Compras Renda Fixa US" page
    //
    // | Date       | Op  | Qty | Price | Tax   | PTAX C | PTAX V |
    // |------------|-----|-----|-------|-------|--------|--------|
    // | 2025-01-10 | BUY | 50  | $50   | $0.25 | 6.0000 |        |
    // | 2025-05-15 | DIV |     | $0.50 | $0    |        | 5.5100 |
    //
    // | Result            | Value |
    // |-------------------|-------|
    // | quantity          | 50    |
    // | dividendsGrossBrl | 2.76  |
    // | dividendsTaxBrl   | 0     |
    @Test
    void getUsStocksIrpf_includesFixedIncome() {
        csvGoogleSheetsClient.setCsv("Compras Renda Fixa US", "/csv/IrpfResource/us_fixed_income_buys.csv");
        csvGoogleSheetsClient.setCsv("Proventos US", "/csv/IrpfResource/us_dividends_fixed_income.csv");

        fakePtaxService.setRate(LocalDate.of(2025, 1, 10), Money.of(new BigDecimal("6.0000"), MoneyUtil.BRL), Money.of(new BigDecimal("6.0100"), MoneyUtil.BRL));
        fakePtaxService.setRate(LocalDate.of(2025, 5, 15), Money.of(new BigDecimal("5.5000"), MoneyUtil.BRL), Money.of(new BigDecimal("5.5100"), MoneyUtil.BRL));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData tflo = result.stream().filter(d -> d.symbol().equals("TFLO")).findFirst().orElseThrow();
        assertThat(tflo.quantity()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(tflo.dividendsGrossBrl()).isEqualTo(brl("2.76"));
        assertThat(tflo.dividendsTaxBrl()).isEqualTo(brl("0"));
    }

    // Dividend CSV has symbols: AAPL, VNQ, TFLO, "?", "-"
    // Expected: "?" and "-" filtered out
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

    // | Default buys (setUp) | Result                       |
    // |----------------------|------------------------------|
    // | Earliest buy: 2024   | years starts at 2024         |
    // | Current year: now    | years ends at current year   |
    // | All years contiguous | no gaps                      |
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

    // ATVI: bought 3.33333 + 6.66667 = ~10, sold 10 → quantity ≈ 0
    // Expected: excluded (near-zero after rounding)
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

    // Sells CSV has symbol "-", dividends CSV has "?", "", "Estorno Impostos sobre Dividendos"
    // Expected: all malformed symbols filtered out, AAPL and MSFT present
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

    // | Symbol | Buys | Sells         | Expected          |
    // |--------|------|---------------|-------------------|
    // | AAPL   | 15   | 3 (valid)     | success, no error |
    // | MSFT   | 8    | 99 (oversell) | error field set   |
    @Test
    void getUsStocksIrpf_returnsErrorFieldForAssetWithCalculationFailure() {
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_oversell.csv");

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData aapl = result.stream().filter(d -> d.symbol().equals("AAPL")).findFirst().orElseThrow();
        assertThat(aapl.error()).isNull();
        assertThat(aapl.avgCostBrl()).isNotNull();

        IrpfAssetData msft = result.stream().filter(d -> d.symbol().equals("MSFT")).findFirst().orElseThrow();
        assertThat(msft.error()).isNotNull();
        assertThat(msft.error()).contains("exceeds position");
    }

    private static Money brl(String value) {
        return Money.of(new BigDecimal(value), MoneyUtil.BRL).with(Monetary.getDefaultRounding());
    }

    private static Money usd(String value) {
        return Money.of(new BigDecimal(value), MoneyUtil.USD).with(Monetary.getDefaultRounding());
    }
}
