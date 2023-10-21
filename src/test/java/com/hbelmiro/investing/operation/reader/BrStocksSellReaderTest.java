package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class BrStocksSellReaderTest extends OperationReaderTest {

    @Inject
    BrStocksSellReader reader;

    @Inject
    CsvGoogleSheetsClient googleSheetsClient;

    @BeforeEach
    void setup() {
        initialize(reader, googleSheetsClient, OperationType.SELL);
    }
}