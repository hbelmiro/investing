package com.hbelmiro.investing.irpf;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;

public record IrpfAssetData(
        String symbol,
        BigDecimal quantity,
        @JsonSerialize(using = MoneySerializer.class) Money avgCostBrl,
        @JsonSerialize(using = MoneySerializer.class) Money totalCostBrl,
        @JsonSerialize(using = MoneySerializer.class) Money capitalGainsBrl,
        @JsonSerialize(using = MoneySerializer.class) Money totalDividendsBrl
) {
}
