package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

@ApplicationScoped
public final class SellReader extends OperationReader {

    static final CurrencyUnit CURRENCY_UNIT = Monetary.getCurrency(Locale.of("pt", "BR"));

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    SellReader() {
    }

    @Inject
    SellReader(GoogleSheetsClient googleSheetsClient) {
        super("Vendas Ações BR", googleSheetsClient, OperationType.SELL, CURRENCY_UNIT);
    }
}
