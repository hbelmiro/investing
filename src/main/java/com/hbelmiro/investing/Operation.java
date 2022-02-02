package com.hbelmiro.investing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Operation(LocalDate date, Stock stock, BigDecimal amount, BigDecimal price, BigDecimal tax , OperationType type) {
}
