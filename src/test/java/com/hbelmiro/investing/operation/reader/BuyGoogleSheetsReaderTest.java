package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class BuyGoogleSheetsReaderTest {

    @Inject
    BuyGoogleSheetsReader reader;

    @Inject
    CsvGoogleSheetsClient googleSheetsClient;

    @Test
    @Disabled
    void test() throws GeneralSecurityException {
        List<Operation> operations = reader.read();
        assertThat(operations).isNotEmpty();
    }

    @Test
    void testRead() throws GeneralSecurityException {
        googleSheetsClient.setCsv("/csv/BuyGoogleSheetsReader/testRead.csv");
        List<Operation> operations = reader.read();

        assertThat(operations).isNotEmpty();
    }
}