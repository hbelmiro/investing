package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
final class SellReader extends OperationReader {

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    SellReader() {
    }

    @Inject
    SellReader(GoogleSheetsClient googleSheetsClient) {
        super("Vendas Ações BR", googleSheetsClient, OperationType.SELL);
    }
}
