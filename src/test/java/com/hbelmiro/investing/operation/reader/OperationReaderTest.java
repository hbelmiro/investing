package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

abstract class OperationReaderTest {

    private OperationReader reader;

    private CsvGoogleSheetsClient googleSheetsClient;

    private OperationType operationType;

    private CurrencyUnit currencyUnit;

    protected final void initialize(OperationReader reader, CsvGoogleSheetsClient googleSheetsClient, OperationType operationType) {
        this.reader = reader;
        this.googleSheetsClient = googleSheetsClient;
        this.operationType = operationType;
        this.currencyUnit = reader.getCurrencyUnit();
    }

    @Test
    void testRead() {
        googleSheetsClient.setCsv("/csv/BuyReader/testRead.csv");
        List<Operation> operations = reader.read();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Operation op1 = Operation.builder()
                .type(operationType)
                .date(LocalDate.parse("15/03/2019", dateTimeFormatter))
                .asset(new Asset("ITUB3", currencyUnit))
                .amount(BigDecimal.valueOf(3))
                .price(Money.of(32.26, currencyUnit))
                .tax(Money.of(0.06, currencyUnit))
                .build();

        Operation op2 = Operation.builder()
                .type(operationType)
                .date(LocalDate.parse("21/03/2019", dateTimeFormatter))
                .asset(new Asset("MDIA3", currencyUnit))
                .amount(BigDecimal.valueOf(-2))
                .price(Money.of(43.90, currencyUnit))
                .tax(Money.of(0.00, currencyUnit))
                .build();

        Operation op3 = Operation.builder()
                .type(operationType)
                .date(LocalDate.parse("21/03/2019", dateTimeFormatter))
                .asset(new Asset("WEGE3", currencyUnit))
                .amount(BigDecimal.valueOf(7))
                .price(Money.of(18.55, currencyUnit))
                .tax(Money.of(0.00, currencyUnit))
                .build();

        assertThat(operations)
                .containsExactlyInAnyOrder(op1, op2, op3);
    }
}
