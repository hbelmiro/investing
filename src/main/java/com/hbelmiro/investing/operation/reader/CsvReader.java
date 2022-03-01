package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.Stock;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

abstract class CsvReader {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String CURRENCY_SYMBOL_PREFIX = "R$ ";

    private final String path;

    private final OperationType operationType;

    protected CsvReader(String path, OperationType operationType) {
        this.path = path;
        this.operationType = operationType;
    }

    List<Operation> read() {
        var classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Cannot find file " + path);
            }

            return parse(new String(is.readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Operation> parse(String fileContent) {
        return Stream.of(fileContent.split(System.lineSeparator()))
                     .skip(2)
                     .map(this::parseLine)
                     .toList();
    }

    private Operation parseLine(String line) {
        String[] splitLine = line.split(";");

        var date = LocalDate.parse(splitLine[0], DATE_TIME_FORMATTER);
        var stock = new Stock(splitLine[1]);
        var amount = new BigDecimal(splitLine[2]);
        var price = new BigDecimal(splitLine[3].replace("\"", "").replace(",", ".").replace(CURRENCY_SYMBOL_PREFIX, ""));
        var tax1 = new BigDecimal(splitLine[4].replace("\"", "").replace(",", ".").replace(CURRENCY_SYMBOL_PREFIX, ""));
        var tax2 = new BigDecimal(splitLine[5].replace("\"", "").replace(",", ".").replace(CURRENCY_SYMBOL_PREFIX, ""));
        return new Operation(date, stock, amount, price, tax1.add(tax2), operationType);
    }
}
