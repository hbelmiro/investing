package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.currency.CurrencyCode;
import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DividendReaderTest {

    private static final CurrencyUnit BRL = Monetary.getCurrency(CurrencyCode.BRL);

    private static final CurrencyUnit USD = Monetary.getCurrency(CurrencyCode.USD);

    @Inject
    CsvGoogleSheetsClient csvGoogleSheetsClient;

    @Inject
    BrDividendReader dividendReader;

    @Inject
    UsDividendReader usDividendReader;

    @Test
    void read() {
        csvGoogleSheetsClient.setCsv("/csv/DividendReader/read.csv");

        List<Dividend> dividends = dividendReader.read();

        Dividend d1 = new Dividend(
                LocalDate.of(2019, 3, 15),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("32.26"), BRL),
                Money.of(new BigDecimal("0.05"), BRL),
                new Asset("ITUB3", BRL)
        );

        Dividend d2 = new Dividend(
                LocalDate.of(2019, 3, 21),
                DividendType.INTEREST_ON_EQUITY,
                Money.of(new BigDecimal("43.90"), BRL),
                Money.zero(BRL),
                new Asset("MDIA3", BRL)
        );

        Dividend d3 = new Dividend(
                LocalDate.of(2019, 3, 21),
                DividendType.FRACTIONS,
                Money.of(new BigDecimal("18.55"), BRL),
                Money.zero(BRL),
                new Asset("WEGE3", BRL)
        );

        assertThat(dividends)
                .containsExactlyInAnyOrder(d1, d2, d3);
    }

    @Test
    void readYearMonth() {
        csvGoogleSheetsClient.setCsv("/csv/DividendReader/readYearMonth.csv");

        List<Dividend> dividends = dividendReader.read(YearMonth.of(2019, Month.MARCH));

        Dividend d1 = new Dividend(
                LocalDate.of(2019, 3, 15),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("32.26"), BRL),
                Money.of(new BigDecimal("0.05"), BRL),
                new Asset("ITUB3", BRL)
        );

        Dividend d2 = new Dividend(
                LocalDate.of(2019, 3, 21),
                DividendType.INTEREST_ON_EQUITY,
                Money.of(new BigDecimal("43.90"), BRL),
                Money.zero(BRL),
                new Asset("MDIA3", BRL)
        );

        assertThat(dividends)
                .containsExactlyInAnyOrder(d1, d2);
    }

    @Test
    void readUsDividends() {
        csvGoogleSheetsClient.setCsv("/csv/UsDividendReader/read.csv");

        List<Dividend> dividends = usDividendReader.read();

        Dividend d1 = new Dividend(
                LocalDate.of(2019, 3, 15),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("0.75"), USD),
                Money.of(new BigDecimal("0.05"), USD),
                new Asset("AAPL", USD)
        );

        Dividend d2 = new Dividend(
                LocalDate.of(2019, 3, 21),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("1.20"), USD),
                Money.zero(USD),
                new Asset("VOO", USD)
        );

        Dividend d3 = new Dividend(
                LocalDate.of(2019, 3, 21),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("0.51"), USD),
                Money.zero(USD),
                new Asset("MSFT", USD)
        );

        assertThat(dividends)
                .containsExactlyInAnyOrder(d1, d2, d3);
    }

    @Test
    void readUsDividendsYearMonth() {
        csvGoogleSheetsClient.setCsv("/csv/UsDividendReader/readYearMonth.csv");

        List<Dividend> dividends = usDividendReader.read(YearMonth.of(2019, Month.MARCH));

        Dividend d1 = new Dividend(
                LocalDate.of(2019, 3, 15),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("0.75"), USD),
                Money.of(new BigDecimal("0.05"), USD),
                new Asset("AAPL", USD)
        );

        Dividend d2 = new Dividend(
                LocalDate.of(2019, 3, 21),
                DividendType.DIVIDEND,
                Money.of(new BigDecimal("1.20"), USD),
                Money.zero(USD),
                new Asset("VOO", USD)
        );

        assertThat(dividends)
                .containsExactlyInAnyOrder(d1, d2);
    }
}
