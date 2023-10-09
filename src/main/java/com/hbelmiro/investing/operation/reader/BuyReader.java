package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

@ApplicationScoped
final class BuyReader extends OperationReader {

    static final CurrencyUnit CURRENCY_UNIT = Monetary.getCurrency(Locale.of("pt", "BR"));

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    private BuyReader() {
    }

    @Inject
    BuyReader(GoogleSheetsClient googleSheetsClient) {
        super("Compras Ações BR", googleSheetsClient, OperationType.BUY, CURRENCY_UNIT);
    }
}
