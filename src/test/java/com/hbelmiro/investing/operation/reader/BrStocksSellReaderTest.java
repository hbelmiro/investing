package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class BrStocksSellReaderTest extends OperationReaderTest {

    @Inject
    BrStocksSellReaderTest(BrStocksSellReader reader, CsvGoogleSheetsClient googleSheetsClient) {
        super(reader, googleSheetsClient, OperationType.SELL);
    }
}