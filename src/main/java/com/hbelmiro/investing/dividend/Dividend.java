package com.hbelmiro.investing.dividend;

import com.hbelmiro.investing.asset.Asset;
import org.javamoney.moneta.Money;

import java.time.LocalDate;

public record Dividend(LocalDate date, DividendType type, Money value, Asset asset) {
}
