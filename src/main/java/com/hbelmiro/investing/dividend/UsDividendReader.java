package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class UsDividendReader extends DividendReader {

    /**
     * @deprecated must be used only by CDI
     */
    @Deprecated(forRemoval = true)
    UsDividendReader() {
        super(null, null, null);
    }

    @Inject
    public UsDividendReader(GoogleSheetsClient googleSheetsClient) {
        super(googleSheetsClient, "Proventos US", MoneyUtil.USD);
    }
}
