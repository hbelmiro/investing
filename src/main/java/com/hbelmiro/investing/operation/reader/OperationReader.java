package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.Stock;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

abstract class OperationReader {

    public static final String RANGE = "A3:F";

    private final String page;

    private final GoogleSheetsClient googleSheetsClient;

    private final OperationType operationType;

    protected OperationReader() {
        // Needed for CDI
        page = null;
        googleSheetsClient = null;
        operationType = null;
    }

    OperationReader(String page, GoogleSheetsClient googleSheetsClient, OperationType operationType) {
        this.page = page;
        this.googleSheetsClient = googleSheetsClient;
        this.operationType = operationType;
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
                .stock(new Stock(row.get(1).toString()))
                .amount(new BigDecimal(row.get(2).toString().replace(".", "").replace(",", ".")))
                .price(toMoney(row.get(3).toString()))
                .tax(toMoney(row.get(4).toString()).add(toMoney(row.get(5).toString())))
                .build();
    }

    private static BigDecimal toMoney(String value) {
        return new BigDecimal(value.replace("R$ ", "").replace(".", "").replace(",", "."));
    }
}