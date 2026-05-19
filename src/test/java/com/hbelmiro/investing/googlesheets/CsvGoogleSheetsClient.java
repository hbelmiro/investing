package com.hbelmiro.investing.googlesheets;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@IfBuildProfile("test")
public class CsvGoogleSheetsClient implements GoogleSheetsClient {

    private String csv;
    private final Map<String, String> pageCsvMap = new HashMap<>();

    @Override
    public List<List<Object>> read(String page, String range) {
        String csvPath = pageCsvMap.getOrDefault(page, csv);
        if (csvPath == null) {
            throw new IllegalStateException("CSV not set.");
        }
        return readCsv(csvPath);
    }

    private List<List<Object>> readCsv(String csvPath) {
        try (InputStream inputStream = getClass().getResourceAsStream(csvPath)) {
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
                throw new UncheckedIOException(new FileNotFoundException(csvPath));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String removeQuotes(String value) {
        return value.replaceAll("^\"|\"$", "");
    }

    public void setCsv(String csv) {
        this.csv = csv;
        pageCsvMap.clear();
    }

    public void setCsv(String page, String csvPath) {
        pageCsvMap.put(page, csvPath);
    }
}
