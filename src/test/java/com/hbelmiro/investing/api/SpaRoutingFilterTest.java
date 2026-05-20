package com.hbelmiro.investing.api;

import com.hbelmiro.investing.googlesheets.CsvGoogleSheetsClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SpaRoutingFilterTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Inject
    CsvGoogleSheetsClient csvGoogleSheetsClient;

    @BeforeEach
    void setUp() {
        csvGoogleSheetsClient.setCsv("Compras Ações US", "/csv/IrpfResource/us_buys.csv");
        csvGoogleSheetsClient.setCsv("Compras Renda Fixa US", "/csv/IrpfResource/empty.csv");
        csvGoogleSheetsClient.setCsv("Compras REITS", "/csv/IrpfResource/empty.csv");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/irpf", "/some-unknown-route"})
    void spaRoutes_serveIndexHtml(String path) throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:8081" + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<div id=\"root\">");
    }

    @Test
    void apiRoutes_passThrough() throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:8081/api/irpf/years")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).startsWith("[");
    }

    @Test
    void staticFiles_passThrough() throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:8081/index.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<div id=\"root\">");
    }
}
