package com.hbelmiro.investing.ptax;

import java.math.BigDecimal;

public record PtaxRate(BigDecimal cotacaoCompra, BigDecimal cotacaoVenda, String dataHoraCotacao) {
}
