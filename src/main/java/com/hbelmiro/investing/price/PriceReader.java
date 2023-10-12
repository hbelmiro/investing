package com.hbelmiro.investing.price;

import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.asset.AssetNotFoundException;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.googlesheets.ReadingException;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@ApplicationScoped
public class PriceReader {

    private static final String RANGE = "A2:C";

    private static final String PAGE = "Cotações";

    private static final int ASSET = 0;

    private static final int PRICE = 1;

    private static final int CURRENCY_UNIT = 2;

    private final GoogleSheetsClient googleSheetsClient;

    PriceReader(GoogleSheetsClient googleSheetsClient) {
        this.googleSheetsClient = googleSheetsClient;
    }

    public Money read(Asset asset) {
        final List<List<Object>> rows;
        try {
            rows = googleSheetsClient.read(PAGE, RANGE);
        } catch (IOException | GeneralSecurityException e) {
            throw new ReadingException("Error reading current price of " + asset.symbol(), e);
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
        return MoneyUtil.toMoney(row.get(PRICE).toString(), row.get(CURRENCY_UNIT).toString());
    }
}
