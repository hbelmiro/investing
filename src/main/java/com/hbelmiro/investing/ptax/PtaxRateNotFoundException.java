package com.hbelmiro.investing.ptax;

import java.time.LocalDate;

public class PtaxRateNotFoundException extends RuntimeException {

    public PtaxRateNotFoundException(LocalDate date, int maxLookbackDays) {
        super("Could not find PTAX rate for date " + date + " after " + maxLookbackDays + " days of lookback.");
    }
}
