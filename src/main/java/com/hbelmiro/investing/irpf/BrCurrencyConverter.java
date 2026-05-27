package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.dividend.Dividend;
import org.javamoney.moneta.Money;

public final class BrCurrencyConverter implements CurrencyConverter {

    @Override
    public Money toCostBrl(Operation buy) {
        return buy.getPrice().multiply(buy.getAmount()).add(buy.getTax());
    }

    @Override
    public Money toSellBrl(Operation sell) {
        return sell.getPrice().multiply(sell.getAmount());
    }

    @Override
    public Money toDividendGrossBrl(Dividend dividend) {
        return dividend.value();
    }

    @Override
    public Money toDividendTaxBrl(Dividend dividend) {
        return dividend.tax();
    }
}
