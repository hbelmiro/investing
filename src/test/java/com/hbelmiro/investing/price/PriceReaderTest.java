package com.hbelmiro.investing.price;

import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.asset.AssetNotFoundException;
import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import javax.money.Monetary;
import java.security.GeneralSecurityException;

import static com.hbelmiro.investing.currency.CurrencyCode.BRL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
class PriceReaderTest {

    @Inject
    PriceReader priceReader;

    @Inject
    CsvGoogleSheetsClient csvGoogleSheetsClient;

    @Test
    void readPrice() throws GeneralSecurityException {
        csvGoogleSheetsClient.setCsv("/csv/PriceReader/readPrice.csv");

        Money price = priceReader.read(new Asset("ITUB3", Monetary.getCurrency(BRL)));
        assertThat(price).isNotNull();
    }

    @Test
    void readNotAvailablePrice() {
        csvGoogleSheetsClient.setCsv("/csv/PriceReader/readPrice.csv");

        Asset enbr3 = new Asset("ENBR3", Monetary.getCurrency(BRL));

        assertThatExceptionOfType(AssetNotFoundException.class)
                .isThrownBy(() -> priceReader.read(enbr3));
    }

    @Test
    void readNonexistentAsset() {
        csvGoogleSheetsClient.setCsv("/csv/PriceReader/readPrice.csv");

        Asset nonExistentAsset = new Asset("HELBER", Monetary.getCurrency(BRL));

        assertThatExceptionOfType(AssetNotFoundException.class)
                .isThrownBy(() -> priceReader.read(nonExistentAsset));
    }
}