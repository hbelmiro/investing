package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.googlesheets.ReadingException;
import com.hbelmiro.investing.utils.MoneyUtil;

import javax.money.CurrencyUnit;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class DividendReader {

    protected record ColumnMapping(String range, int date, int asset, int value, int tax, int type) {}

    private final GoogleSheetsClient googleSheetsClient;

    private final String page;

    private final CurrencyUnit currencyUnit;

    private final ColumnMapping columnMapping;

    protected DividendReader(GoogleSheetsClient googleSheetsClient, String page, CurrencyUnit currencyUnit, ColumnMapping columnMapping) {
        this.googleSheetsClient = googleSheetsClient;
        this.page = page;
        this.currencyUnit = currencyUnit;
        this.columnMapping = columnMapping;
    }

    public List<Dividend> read() {
        final List<List<Object>> rows;
        try {
            rows = googleSheetsClient.read(page, columnMapping.range());
        } catch (IOException | GeneralSecurityException e) {
            throw new ReadingException("Error reading dividends.", e);
        }

        return rows.stream()
                .map(this::toDividend)
                .toList();
    }

    public List<Dividend> read(YearMonth yearMonth) {
        return read().stream()
                .filter(dividend -> YearMonth.of(dividend.date().getYear(), dividend.date().getMonth()).equals(yearMonth))
                .toList();
    }

    private Dividend toDividend(List<Object> row) {
        return new Dividend(
                LocalDate.parse(row.get(columnMapping.date()).toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                toDividendType(row.get(columnMapping.type()).toString()),
                MoneyUtil.toMoney(row.get(columnMapping.value()).toString(), currencyUnit),
                MoneyUtil.toMoney(row.get(columnMapping.tax()).toString(), currencyUnit),
                new Asset(row.get(columnMapping.asset()).toString(), currencyUnit)
        );
    }

    private static DividendType toDividendType(String type) {
        return switch (type) {
            case "Dividendo", "Dividendos", "Rendimento", "Estorno Impostos sobre Dividendos", "Diferença Avenue", "?" -> DividendType.DIVIDEND;
            case "JCP" -> DividendType.INTEREST_ON_EQUITY;
            case "Frações" -> DividendType.FRACTIONS;
            case "30% Imposto/Dividendo" -> DividendType.TAX;
            default -> throw new UnsupportedOperationException("Unsupported dividend type: " + type);
        };
    }
}
