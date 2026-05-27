package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class FiiBuyReader extends OperationReader {

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    private FiiBuyReader() {
    }

    @Inject
    FiiBuyReader(GoogleSheetsClient googleSheetsClient) {
        super("Compras FIIs", googleSheetsClient, OperationType.BUY, MoneyUtil.BRL);
    }
}
