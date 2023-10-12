package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.googlesheets.ReadingException;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class DividendReader {

    private static final String RANGE = "A2:D";

    private static final String PAGE = "Proventos BR";

    private static final int DATE = 0;

    private static final int ASSET = 1;

    private static final int VALUE = 2;

    private static final int TYPE = 3;

    private final GoogleSheetsClient googleSheetsClient;

    public DividendReader(GoogleSheetsClient googleSheetsClient) {
        this.googleSheetsClient = googleSheetsClient;
    }

    public List<Dividend> read() {
        final List<List<Object>> rows;
        try {
            rows = googleSheetsClient.read(PAGE, RANGE);
        } catch (IOException | GeneralSecurityException e) {
            throw new ReadingException("Error reading dividends.", e);
        }

        return rows.stream()
                .map(DividendReader::toDividend)
                .toList();
    }

    private static Dividend toDividend(List<Object> row) {
        return new Dividend(
                LocalDate.parse(row.get(DATE).toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                toDividendType(row.get(TYPE).toString()),
                toMoney(row.get(VALUE).toString()),
                new Asset(row.get(ASSET).toString(), MoneyUtil.BRL)
        );
    }

    private static DividendType toDividendType(String type) {
        return switch (type) {
            case "Dividendos", "Rendimento" -> DividendType.DIVIDEND;
            case "JCP" -> DividendType.INTEREST_ON_EQUITY;
            case "Frações" -> DividendType.FRACTIONS;
            default -> throw new UnsupportedOperationException("Unsupported dividend type: " + type);
        };
    }

    private static Money toMoney(String value) {
        return MoneyUtil.toBrazilianMoney(value);
    }

}
