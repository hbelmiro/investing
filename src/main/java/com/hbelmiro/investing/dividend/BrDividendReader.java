package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class BrDividendReader extends DividendReader {

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    BrDividendReader() {
        super(null, null, null);
    }

    @Inject
    public BrDividendReader(GoogleSheetsClient googleSheetsClient) {
        super(googleSheetsClient, "Proventos BR", MoneyUtil.BRL);
    }
}
