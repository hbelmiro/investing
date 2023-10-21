package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class UsStocksSellReader extends OperationReader {

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    UsStocksSellReader() {
    }

    @Inject
    UsStocksSellReader(GoogleSheetsClient googleSheetsClient) {
        super("Vendas Ações US", googleSheetsClient, OperationType.SELL, MoneyUtil.USD);
    }
}
