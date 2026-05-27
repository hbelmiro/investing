package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.dividend.Dividend;
import org.javamoney.moneta.Money;

public interface CurrencyConverter {

    Money toCostBrl(Operation buy);

    Money toSellBrl(Operation sell);

    Money toDividendGrossBrl(Dividend dividend);

    Money toDividendTaxBrl(Dividend dividend);
}
