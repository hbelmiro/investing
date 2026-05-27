package com.hbelmiro.investing.irpf;

import org.javamoney.moneta.Money;

public record DividendsResult(Money dividendGrossBrl, Money dividendTaxBrl, Money jcpGrossBrl, Money jcpTaxBrl,
                              Money unknownGrossBrl, Money unknownTaxBrl) {
}
