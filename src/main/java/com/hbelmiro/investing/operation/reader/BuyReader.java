package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
final class BuyReader extends OperationReader {

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    private BuyReader() {
    }

    @Inject
    BuyReader(GoogleSheetsClient googleSheetsClient) {
        super("Compras Ações BR", googleSheetsClient, OperationType.BUY);
    }
}
