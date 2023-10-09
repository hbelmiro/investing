package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class SellReaderTest extends OperationReaderTest {

    @Inject
    SellReaderTest(SellReader reader, CsvGoogleSheetsClient googleSheetsClient) {
        super(reader, googleSheetsClient, OperationType.SELL);
    }
}