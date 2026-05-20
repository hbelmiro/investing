package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class UsReitsSellReader extends OperationReader {

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    UsReitsSellReader() {
    }

    @Inject
    UsReitsSellReader(GoogleSheetsClient googleSheetsClient) {
        super("Vendas REITS", googleSheetsClient, OperationType.SELL, MoneyUtil.USD);
    }
}
