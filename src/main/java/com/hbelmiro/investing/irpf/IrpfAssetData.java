package com.hbelmiro.investing.irpf;

import java.math.BigDecimal;

public record IrpfAssetData(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgCostBrl,
        BigDecimal totalCostBrl,
        BigDecimal capitalGainsBrl,
        BigDecimal totalDividendsBrl
) {
}
