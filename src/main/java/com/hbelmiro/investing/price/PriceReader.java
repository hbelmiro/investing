package com.hbelmiro.investing.price;

import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.asset.AssetNotFoundException;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.List;

@ApplicationScoped
class PriceReader {

    private static final String RANGE = "A2:C";

    private static final String PAGE = "Cotações";

    private static final int ASSET = 0;

    private static final int PRICE = 1;

    private static final int CURRENCY_UNIT = 2;

    private final GoogleSheetsClient googleSheetsClient;

    PriceReader(GoogleSheetsClient googleSheetsClient) {
        this.googleSheetsClient = googleSheetsClient;
    }

    Money read(Asset asset) throws GeneralSecurityException {
        final List<List<Object>> rows;
        try {
            rows = googleSheetsClient.read(PAGE, RANGE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return rows.stream()
                .filter(row -> !row.get(PRICE).equals("#N/A"))
                .filter(row -> row.get(ASSET).equals(asset.symbol()))
                .filter(row -> row.get(CURRENCY_UNIT).equals(asset.currencyUnit().getCurrencyCode()))
                .findAny()
                .map(PriceReader::toMoney)
                .orElseThrow(() -> new AssetNotFoundException(asset));
    }

    private static Money toMoney(List<Object> row) {
        CurrencyUnit currencyUnit = Monetary.getCurrency(row.get(CURRENCY_UNIT).toString());
        BigDecimal price = new BigDecimal(row.get(PRICE).toString().replace(',', '.'));

        return Money.of(price, currencyUnit);
    }
}
