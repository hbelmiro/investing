package com.hbelmiro.investing.googlesheets;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CsvGoogleSheetsClientTest {

    @Inject
    CsvGoogleSheetsClient client;

    @Test
    void read_emptyFile_returnsEmptyList() {
        client.setCsv("/csv/IrpfResource/empty.csv");
        List<List<Object>> result = client.read("anyPage", "A2:F");
        assertThat(result).isEmpty();
    }

    @Test
    void read_perPageMapping_overridesGlobal() {
        client.setCsv("/csv/BuyReader/testRead.csv");
        client.setCsv("CustomPage", "/csv/IrpfResource/empty.csv");

        List<List<Object>> globalResult = client.read("OtherPage", "A2:F");
        assertThat(globalResult).isNotEmpty();

        List<List<Object>> pageResult = client.read("CustomPage", "A2:F");
        assertThat(pageResult).isEmpty();
    }
}
