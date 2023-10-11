package com.hbelmiro.investing.asset;

import javax.money.CurrencyUnit;

public record Asset(String symbol, CurrencyUnit currencyUnit) {
}
