package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class BrStocksBuyReaderTest extends OperationReaderTest {

    @Inject
    BrStocksBuyReader reader;

    @Inject
    CsvGoogleSheetsClient googleSheetsClient;

    @BeforeEach
    void setup() {
        initialize(reader, googleSheetsClient, OperationType.BUY);
    }
}
