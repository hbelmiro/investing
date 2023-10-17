package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;

import javax.money.CurrencyUnit;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.hbelmiro.investing.utils.MoneyUtil.toMoney;

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

    List<Operation> read() throws GeneralSecurityException {
        try {
            return googleSheetsClient.read(page, RANGE).stream()
                    .map(this::toOperation)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Operation toOperation(List<Object> row) {
        return Operation.builder()
                .type(operationType)
                .date(LocalDate.parse(row.get(0).toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .stock(new Asset(row.get(1).toString(), currencyUnit))
                .amount(new BigDecimal(row.get(2).toString().replace(".", "").replace(",", ".")))
                .price(toMoney(row.get(3).toString(), currencyUnit))
                .tax(toMoney(row.get(4).toString(), currencyUnit).add(toMoney(row.get(5).toString(), currencyUnit)))
                .build();
    }
}
