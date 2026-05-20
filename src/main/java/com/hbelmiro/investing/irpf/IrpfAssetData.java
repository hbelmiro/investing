package com.hbelmiro.investing.irpf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IrpfAssetData(
        String symbol,
        BigDecimal quantity,
        @JsonSerialize(using = MoneySerializer.class) Money avgCostBrl,
        @JsonSerialize(using = MoneySerializer.class) Money totalCostBrl,
        @JsonSerialize(using = MoneySerializer.class) Money avgCostUsd,
        @JsonSerialize(using = MoneySerializer.class) Money totalCostUsd,
        @JsonSerialize(using = MoneySerializer.class) Money ptaxRate,
        @JsonSerialize(using = MoneySerializer.class) Money capitalGainsBrl,
        @JsonSerialize(using = MoneySerializer.class) Money totalCapitalGainsBrl,
        @JsonSerialize(using = MoneySerializer.class) Money dividendsGrossBrl,
        @JsonSerialize(using = MoneySerializer.class) Money dividendsTaxBrl,
        String error
) {

    public static IrpfAssetData error(String symbol, String error) {
        return new IrpfAssetData(symbol, null, null, null, null, null, null, null, null, null, null, error);
    }
}
