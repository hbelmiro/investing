package com.hbelmiro.investing.ptax;

import com.hbelmiro.investing.utils.MoneyUtil;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@IfBuildProfile("test")
public class FakePtaxService implements PtaxService {

    private static final String DEFAULT_TIMESTAMP_SUFFIX = " 13:00:00.000";
    private final Map<LocalDate, PtaxRate> rates = new HashMap<>();

    @SuppressWarnings("unused")
    public void setRate(LocalDate date, BigDecimal cotacaoCompra, BigDecimal cotacaoVenda) {
        String dataHoraCotacao = date.format(DateTimeFormatter.ISO_LOCAL_DATE) + DEFAULT_TIMESTAMP_SUFFIX;
        rates.put(date, new PtaxRate(cotacaoCompra, cotacaoVenda, dataHoraCotacao));
    }

    @Override
    public Money getCotacaoCompra(LocalDate date) {
        PtaxRate rate = rates.get(date);
        if (rate == null) {
            throw new PtaxRateNotFoundException(date, 0);
        }
        return Money.of(rate.cotacaoCompra(), MoneyUtil.BRL);
    }

    @Override
    public Money getCotacaoVenda(LocalDate date) {
        PtaxRate rate = rates.get(date);
        if (rate == null) {
            throw new PtaxRateNotFoundException(date, 0);
        }
        return Money.of(rate.cotacaoVenda(), MoneyUtil.BRL);
    }

}
