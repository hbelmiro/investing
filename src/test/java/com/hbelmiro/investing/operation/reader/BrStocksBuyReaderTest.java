package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class BrStocksBuyReaderTest extends OperationReaderTest {

    @Inject
    BrStocksBuyReaderTest(BrStocksBuyReader reader, CsvGoogleSheetsClient googleSheetsClient) {
        super(reader, googleSheetsClient, OperationType.BUY);
    }
}
