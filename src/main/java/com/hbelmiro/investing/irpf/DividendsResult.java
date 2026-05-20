package com.hbelmiro.investing.irpf;

import org.javamoney.moneta.Money;

public record DividendsResult(Money grossBrl, Money taxBrl) {
}
