package com.hbelmiro.investing.ptax;

import org.javamoney.moneta.Money;

import java.time.LocalDate;

public interface PtaxService {

    Money getCotacaoCompra(LocalDate date);

    Money getCotacaoVenda(LocalDate date);
}
