package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.googlesheets.ReadingException;
import com.hbelmiro.investing.utils.MoneyUtil;

import javax.money.CurrencyUnit;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class DividendReader {

    private static final String RANGE = "A2:F";

    private static final int DATE = 0;

    private static final int ASSET = 1;

    private static final int VALUE = 2;

    private static final int TAX = 3;

    private static final int TYPE = 5;

    private final GoogleSheetsClient googleSheetsClient;

    private final String page;

    private final CurrencyUnit currencyUnit;

    protected DividendReader(GoogleSheetsClient googleSheetsClient, String page, CurrencyUnit currencyUnit) {
        this.googleSheetsClient = googleSheetsClient;
        this.page = page;
        this.currencyUnit = currencyUnit;
    }

    public List<Dividend> read() {
        final List<List<Object>> rows;
        try {
            rows = googleSheetsClient.read(page, RANGE);
        } catch (IOException | GeneralSecurityException e) {
            throw new ReadingException("Error reading dividends.", e);
        }

        return rows.stream()
                .map(this::toDividend)
                .toList();
    }

    private Dividend toDividend(List<Object> row) {
        return new Dividend(
                LocalDate.parse(row.get(DATE).toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                toDividendType(row.get(TYPE).toString()),
                MoneyUtil.toMoney(row.get(VALUE).toString(), currencyUnit),
                MoneyUtil.toMoney(row.get(TAX).toString(), currencyUnit),
                new Asset(row.get(ASSET).toString(), currencyUnit)
        );
    }

    private static DividendType toDividendType(String type) {
        return switch (type) {
            case "Dividendo", "Dividendos", "Rendimento" -> DividendType.DIVIDEND;
            case "JCP" -> DividendType.INTEREST_ON_EQUITY;
            case "Frações" -> DividendType.FRACTIONS;
            case "30% Imposto/Dividendo" -> DividendType.TAX;
            default -> throw new UnsupportedOperationException("Unsupported dividend type: " + type);
        };
    }
}
