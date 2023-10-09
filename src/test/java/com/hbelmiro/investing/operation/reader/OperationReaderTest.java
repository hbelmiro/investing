package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.Stock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

abstract class OperationReaderTest {

    private final OperationReader reader;

    private final CsvGoogleSheetsClient googleSheetsClient;

    private final OperationType operationType;

    protected OperationReaderTest(OperationReader reader, CsvGoogleSheetsClient googleSheetsClient, OperationType operationType) {
        this.reader = reader;
        this.googleSheetsClient = googleSheetsClient;
        this.operationType = operationType;
    }

    @Test
    void testRead() throws GeneralSecurityException {
        googleSheetsClient.setCsv("/csv/BuyReader/testRead.csv");
        List<Operation> operations = reader.read();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Operation op1 = Operation.builder()
                .type(operationType)
                .date(LocalDate.parse("15/03/2019", dateTimeFormatter))
                .stock(new Stock("ITUB3"))
                .amount(BigDecimal.valueOf(3))
                .price(new BigDecimal("32.26"))
                .tax(new BigDecimal("0.06"))
                .build();

        Operation op2 = Operation.builder()
                .type(operationType)
                .date(LocalDate.parse("21/03/2019", dateTimeFormatter))
                .stock(new Stock("MDIA3"))
                .amount(BigDecimal.valueOf(-2))
                .price(new BigDecimal("43.90"))
                .tax(new BigDecimal("0.00"))
                .build();

        Operation op3 = Operation.builder()
                .type(operationType)
                .date(LocalDate.parse("21/03/2019", dateTimeFormatter))
                .stock(new Stock("WEGE3"))
                .amount(BigDecimal.valueOf(7))
                .price(new BigDecimal("18.55"))
                .tax(new BigDecimal("0.00"))
                .build();

        assertThat(operations)
                .containsExactlyInAnyOrder(op1, op2, op3);
    }
}
