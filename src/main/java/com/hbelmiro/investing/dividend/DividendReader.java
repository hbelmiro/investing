package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class DividendReader {

    private static final String RANGE = "A2:D";

    private static final String PAGE = "Proventos BR";

    private static final int DATE = 0;

    private static final int ASSET = 1;

    private static final int VALUE = 2;

    private static final int TYPE = 3;

    private static final CurrencyUnit CURRENCY_UNIT = Monetary.getCurrency(Locale.of("pt", "BR"));

    private final GoogleSheetsClient googleSheetsClient;

    public DividendReader(GoogleSheetsClient googleSheetsClient) {
        this.googleSheetsClient = googleSheetsClient;
    }

    List<Dividend> read() throws GeneralSecurityException {
        final List<List<Object>> rows;
        try {
            rows = googleSheetsClient.read(PAGE, RANGE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return rows.stream()
                .map(DividendReader::toDividend)
                .toList();
    }

    private static Dividend toDividend(List<Object> row) {
        return new Dividend(
                LocalDate.parse(row.get(0).toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                toDividendType(row.get(3).toString()),
                toMoney(row.get(2).toString()),
                new Asset(row.get(1).toString(), CURRENCY_UNIT)
        );
    }

    private static DividendType toDividendType(String type) {
        return switch (type) {
            case "Dividendos" -> DividendType.DIVIDEND;
            case "JCP" -> DividendType.INTEREST_ON_EQUITY;
            case "Fr" -> DividendType.FRACTIONS;
            default -> throw new UnsupportedOperationException("Unsupported dividend type: " + type);
        };
    }

    private static Money toMoney(String value) {
        var bigDecimal = new BigDecimal(value.replace("R$ ", "").replace(".", "").replace(",", "."));
        return Money.of(bigDecimal, CURRENCY_UNIT);
    }

}
