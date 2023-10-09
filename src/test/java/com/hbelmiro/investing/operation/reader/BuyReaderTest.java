package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class BuyReaderTest extends OperationReaderTest {

    @Inject
    BuyReaderTest(BuyReader reader, CsvGoogleSheetsClient googleSheetsClient) {
        super(reader, googleSheetsClient, OperationType.BUY, BuyReader.CURRENCY_UNIT);
    }
}
