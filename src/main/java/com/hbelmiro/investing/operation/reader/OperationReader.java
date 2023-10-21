package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import com.hbelmiro.investing.googlesheets.ReadingException;

import javax.money.CurrencyUnit;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.hbelmiro.investing.utils.MoneyUtil.toMoney;

abstract class OperationReader {

    private static final String RANGE = "A2:F";

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

    CurrencyUnit getCurrencyUnit() {
        return currencyUnit;
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
                .price(toMoney(row.get(3).toString(), currencyUnit))
                .tax(toMoney(row.get(4).toString(), currencyUnit))
                .build();
    }
}
