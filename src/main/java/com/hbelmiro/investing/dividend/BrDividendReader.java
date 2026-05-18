package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class BrDividendReader extends DividendReader {

    private static final ColumnMapping COLUMN_MAPPING = new ColumnMapping("A2:F", 0, 1, 2, 3, 5);

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    BrDividendReader() {
        super(null, null, null, null);
    }

    @Inject
    public BrDividendReader(GoogleSheetsClient googleSheetsClient) {
        super(googleSheetsClient, "Proventos BR", MoneyUtil.BRL, COLUMN_MAPPING);
    }
}
