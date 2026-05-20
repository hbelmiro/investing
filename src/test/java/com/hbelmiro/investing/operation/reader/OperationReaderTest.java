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

import static com.hbelmiro.investing.utils.MoneyUtil.toMoney;
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

    // | Date       | Symbol | Qty | Price | Tax  |
    // |------------|--------|-----|-------|------|
    // | 15/03/2019 | ITUB3  | 3   | 32.26 | 0.06 |
    // | 21/03/2019 | MDIA3  | -2  | 43.90 | 0.00 |
    // | 21/03/2019 | WEGE3  | 7   | 18.55 | 0.00 |
    //
    // Expected: 3 operations parsed correctly
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

    // | Row | Content                                     |
    // |-----|---------------------------------------------|
    // | 1   | 15/03/2019;ITUB3;3;"R$ 32,26";"R$ 0,06"... |
    // | 2   | (empty line)                                |
    // | 3   | 21/03/2019;WEGE3;7;"R$ 18,55";"R$ 0,00"... |
    //
    // Expected: 2 operations (empty row skipped)
    @Test
    void testRead_skipsEmptyRows() {
        googleSheetsClient.setCsv("/csv/BuyReader/testReadWithEmptyRows.csv");
        List<Operation> operations = reader.read();

        assertThat(operations).hasSize(2);
        assertThat(operations).extracting(op -> op.getAsset().symbol())
                .containsExactlyInAnyOrder("ITUB3", "WEGE3");
    }

    // | Row | Columns present         | Tax column? |
    // |-----|-------------------------|-------------|
    // | 1   | date;ITUB3;3;"R$ 32,26" | missing     |
    // | 2   | date;WEGE3;7;...;tax    | present     |
    //
    // Expected: ITUB3 tax defaults to 0
    @Test
    void testRead_defaultsTaxToZeroWhenMissing() {
        googleSheetsClient.setCsv("/csv/BuyReader/testReadWithMissingTax.csv");
        List<Operation> operations = reader.read();

        assertThat(operations).hasSize(2);

        Operation itub = operations.stream()
                .filter(op -> op.getAsset().symbol().equals("ITUB3"))
                .findFirst().orElseThrow();
        assertThat(itub.getTax()).isEqualTo(toMoney("0", currencyUnit));
    }
}
