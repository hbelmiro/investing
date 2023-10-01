package com.hbelmiro.investing;

import java.math.BigDecimal;

public record Indicators(BigDecimal dividendYield, BigDecimal lpa, BigDecimal netWorth, BigDecimal shares) {

}
