package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class UsDividendReader extends DividendReader {

    private static final ColumnMapping COLUMN_MAPPING = new ColumnMapping("A2:N", 0, 2, 3, 4, 6);

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    UsDividendReader() {
        super(null, null, null, null);
    }

    @Inject
    public UsDividendReader(GoogleSheetsClient googleSheetsClient) {
        super(googleSheetsClient, "Proventos US", MoneyUtil.USD, COLUMN_MAPPING);
    }
}
