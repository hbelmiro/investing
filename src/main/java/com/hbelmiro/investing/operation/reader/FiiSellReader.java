package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class FiiSellReader extends OperationReader {

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    FiiSellReader() {
    }

    @Inject
    FiiSellReader(GoogleSheetsClient googleSheetsClient) {
        super("Vendas FIIs", googleSheetsClient, OperationType.SELL, MoneyUtil.BRL);
    }
}
