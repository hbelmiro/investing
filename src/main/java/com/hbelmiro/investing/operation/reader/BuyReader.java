package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.Stock;
import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
final class BuyReader {

    private static final String PAGE = "Compras Ações BR";

    public static final String RANGE = "A3:F";

    private final GoogleSheetsClient googleSheetsClient;

    BuyReader(GoogleSheetsClient googleSheetsClient) {
        this.googleSheetsClient = googleSheetsClient;
    }

    List<Operation> read() throws GeneralSecurityException {
        try {
            return googleSheetsClient.read(PAGE, RANGE).stream()
                    .map(BuyReader::toOperation)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Operation toOperation(List<Object> row) {
        return Operation.builder()
                .type(OperationType.BUY)
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
