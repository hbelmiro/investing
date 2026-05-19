package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import com.hbelmiro.investing.ptax.FakePtaxService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        csvGoogleSheetsClient.setCsv("Proventos US", "/csv/IrpfResource/us_dividends.csv");

        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), new BigDecimal("5.4000"), new BigDecimal("5.4100"));
        fakePtaxService.setRate(LocalDate.of(2025, 1, 15), new BigDecimal("6.0370"), new BigDecimal("6.0380"));
        fakePtaxService.setRate(LocalDate.of(2025, 3, 15), new BigDecimal("5.5000"), new BigDecimal("5.5100"));
        fakePtaxService.setRate(LocalDate.of(2025, 6, 15), new BigDecimal("5.7000"), new BigDecimal("5.7100"));
    }

    @Test
    void getUsStocksIrpf() {
        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        assertThat(result).hasSize(2);

        // AAPL: buys in 2024 + 2025, sell in 2025, dividend in 2025
        // avgCostBrl: (500.50*5.4000 + 300.30*5.5000) / 15 = (2702.70 + 1651.65) / 15 = 290.29
        // capitalGains: sellBrl(70*3*5.7100=1199.10) - costBrl(290.29*3=870.87) = 328.23
        // quantity: 15 bought - 3 sold = 12
        // totalCostBrl: 290.29 * 12 = 3483.48
        // dividendsBrl: (0.75-0.10)*5.7100 = 0.65*5.7100 = 3.71
        IrpfAssetData aapl = result.get(0);
        assertThat(aapl.symbol()).isEqualTo("AAPL");
        assertThat(aapl.quantity()).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(aapl.avgCostBrl()).isEqualByComparingTo(new BigDecimal("290.29"));
        assertThat(aapl.totalCostBrl()).isEqualByComparingTo(new BigDecimal("3483.48"));
        assertThat(aapl.capitalGainsBrl()).isEqualByComparingTo(new BigDecimal("328.23"));
        assertThat(aapl.totalDividendsBrl()).isEqualByComparingTo(new BigDecimal("3.71"));

        // MSFT: buy in 2025, no sells, dividend in 2025
        // avgCostBrl: (320.20*6.0370) / 8 = 1933.0474 / 8 = 241.63
        // capitalGains: 0 (no sells)
        // quantity: 8
        // totalCostBrl: 241.63 * 8 = 1933.04
        // dividendsBrl: 1.20*5.5100 = 6.61
        IrpfAssetData msft = result.get(1);
        assertThat(msft.symbol()).isEqualTo("MSFT");
        assertThat(msft.quantity()).isEqualByComparingTo(new BigDecimal("8"));
        assertThat(msft.avgCostBrl()).isEqualByComparingTo(new BigDecimal("241.63"));
        assertThat(msft.totalCostBrl()).isEqualByComparingTo(new BigDecimal("1933.04"));
        assertThat(msft.capitalGainsBrl()).isEqualByComparingTo(new BigDecimal("0"));
        assertThat(msft.totalDividendsBrl()).isEqualByComparingTo(new BigDecimal("6.61"));
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
        assertThat(aapl.totalDividendsBrl()).isEqualByComparingTo(new BigDecimal("3.71"));
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
        assertThat(aapl.avgCostBrl()).isEqualByComparingTo(new BigDecimal("290.29"));
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
        fakePtaxService.setRate(LocalDate.of(2024, 9, 15), new BigDecimal("5.4500"), new BigDecimal("5.4600"));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        IrpfAssetData aapl = result.stream()
                .filter(d -> d.symbol().equals("AAPL"))
                .findFirst()
                .orElseThrow();

        // AAPL: 15 bought - 2 sold(2024) - 3 sold(2025) = 10
        assertThat(aapl.quantity()).isEqualByComparingTo(new BigDecimal("10"));

        // Capital gains must reflect that 2024 sell depleted position before 2025 sell
        // Chronological: buy1(Jun24), sell1(Sep24, NOT summed), buy2(Mar25), sell2(Jun25, summed)
        // buy1: costBrl=500.50*5.4=2702.70, amount=10, avg=270.27
        // sell1: depletes 2 shares → totalCost=2162.16, amount=8
        // buy2: costBrl=300.30*5.5=1651.65 → totalCost=3813.81, amount=13, avg=293.37
        // sell2: sellBrl=70*3*5.71=1199.10, cost=293.37*3=880.11, gain=318.99
        assertThat(aapl.capitalGainsBrl()).isEqualByComparingTo(new BigDecimal("318.99"));
    }

    @Test
    void getUsStocksIrpf_excludesFullyLiquidatedAssetWithNoYearActivity() {
        csvGoogleSheetsClient.setCsv("Compras Ações US", "/csv/IrpfResource/us_buys_with_liquidated.csv");
        csvGoogleSheetsClient.setCsv("Vendas Ações US", "/csv/IrpfResource/us_sells_with_liquidation.csv");
        fakePtaxService.setRate(LocalDate.of(2024, 3, 15), new BigDecimal("5.0000"), new BigDecimal("5.0100"));
        fakePtaxService.setRate(LocalDate.of(2024, 6, 15), new BigDecimal("5.4000"), new BigDecimal("5.4100"));

        List<IrpfAssetData> result = irpfResource.getUsStocksIrpf(2025);

        // TSLA: bought 10 in 2024, sold 10 in 2024, no 2025 activity → excluded
        assertThat(result).extracting(IrpfAssetData::symbol)
                .containsExactly("AAPL", "MSFT")
                .doesNotContain("TSLA");
    }
}
