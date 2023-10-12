package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.googlesheets.ReadingException;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

abstract class OperationReader {

    private static final String RANGE = "A3:F";

    private final String page;

    private final GoogleSheetsClient googleSheetsClient;

    private final OperationType operationType;

    private final CurrencyUnit currencyUnit;

    protected OperationReader() {
        // Needed for CDI
        page = null;
        googleSheetsClient = null;
        operationType = null;
        currencyUnit = null;
    }

    OperationReader(String page, GoogleSheetsClient googleSheetsClient, OperationType operationType, CurrencyUnit currencyUnit) {
        this.page = page;
        this.googleSheetsClient = googleSheetsClient;
        this.operationType = operationType;
        this.currencyUnit = currencyUnit;
    }

    public List<Operation> read() {
        try {
            return googleSheetsClient.read(page, RANGE).stream()
                    .map(this::toOperation)
                    .toList();
        } catch (IOException | GeneralSecurityException e) {
            throw new ReadingException("Error reading operation.", e);
        }
    }

    private Operation toOperation(List<Object> row) {
        return Operation.builder()
                .type(operationType)
                .date(LocalDate.parse(row.get(0).toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .asset(new Asset(row.get(1).toString(), currencyUnit))
                .amount(new BigDecimal(row.get(2).toString().replace(".", "").replace(",", ".")))
                .price(toMoney(row.get(3).toString()))
                .tax(toMoney(row.get(4).toString()).add(toMoney(row.get(5).toString())))
                .build();
    }

    private Money toMoney(String value) {
        var bigDecimal = new BigDecimal(value.replace("R$ ", "").replace(".", "").replace(",", "."));
        return Money.of(bigDecimal, currencyUnit);
    }
}
