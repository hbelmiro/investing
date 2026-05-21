package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.ptax.PtaxService;
import com.hbelmiro.investing.utils.MoneyUtil;
import org.javamoney.moneta.Money;

public final class UsCurrencyConverter implements CurrencyConverter {

    private final PtaxService ptaxService;

    public UsCurrencyConverter(PtaxService ptaxService) {
        this.ptaxService = ptaxService;
    }

    @Override
    public Money toCostBrl(Operation buy) {
        Money costUsd = buy.getPrice().multiply(buy.getAmount()).add(buy.getTax());
        return convertUsdToBrl(costUsd, ptaxService.getCotacaoCompra(buy.getDate()));
    }

    @Override
    public Money toSellBrl(Operation sell) {
        Money sellUsd = sell.getPrice().multiply(sell.getAmount());
        return convertUsdToBrl(sellUsd, ptaxService.getCotacaoVenda(sell.getDate()));
    }

    @Override
    public Money toDividendGrossBrl(Dividend dividend) {
        return convertUsdToBrl(dividend.value(), ptaxService.getCotacaoVenda(dividend.date()));
    }

    @Override
    public Money toDividendTaxBrl(Dividend dividend) {
        return convertUsdToBrl(dividend.tax(), ptaxService.getCotacaoVenda(dividend.date()));
    }

    private Money convertUsdToBrl(Money usdAmount, Money ptaxRate) {
        return Money.of(usdAmount.multiply(ptaxRate.getNumber()).getNumber(), MoneyUtil.BRL);
    }
}
