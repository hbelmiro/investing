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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DividendReaderTest {

    private static final CurrencyUnit BRL = Monetary.getCurrency(CurrencyCode.BRL);

    @Inject
    CsvGoogleSheetsClient csvGoogleSheetsClient;

    @Inject
    BrDividendReader dividendReader;

    @Test
    void readDividends() {
        csvGoogleSheetsClient.setCsv("/csv/DividendReader/readDividends.csv");

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
}
