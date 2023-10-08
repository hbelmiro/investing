package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.googlesheets.GoogleSheetsClient;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
@IfBuildProfile("test")
class CsvGoogleSheetsClient implements GoogleSheetsClient {

    private String csv;

    @Override
    public List<List<Object>> read(String page, String range) {
        if (csv == null) {
            throw new IllegalStateException("CSV not set.");
        } else {
            try (InputStream inputStream = getClass().getResourceAsStream(csv)) {
                if (inputStream != null) {
                    return Arrays.stream(new String(inputStream.readAllBytes()).split(System.lineSeparator()))
                            .map(row -> {
                                String[] splitValues = row.split(";");
                                return Arrays.stream(splitValues)
                                        .map(CsvGoogleSheetsClient::removeQuotes)
                                        .map(Object.class::cast)
                                        .toList();
                            }).toList();
                } else {
                    throw new UncheckedIOException(new FileNotFoundException(csv));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static String removeQuotes(String value) {
        return value.replaceAll("^\"|\"$", "");
    }

    void setCsv(String csv) {
        this.csv = csv;
    }
}
