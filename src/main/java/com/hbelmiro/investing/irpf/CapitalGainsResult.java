package com.hbelmiro.investing.irpf;

import org.javamoney.moneta.Money;

public record CapitalGainsResult(Money capitalGainsBrl, Money totalCapitalGainsBrl, Money avgCostBrl, Money avgCostUsd) {
}
