package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.Stock;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@ApplicationScoped
final class BuyCsvReader {

    private static final MathContext CURRENCY_MATH_CONTEXT = new MathContext(2, RoundingMode.HALF_UP);

    private static final OperationType OPERATION_TYPE = OperationType.BUY;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String CURRENCY_SYMBOL_PREFIX = "R$ ";

    private final NumberFormat nf = NumberFormat.getInstance(new Locale("pt", "BR"));

    @ConfigProperty(name = "operation.reader.csv.buy.path")
    private String path;

    List<Operation> read() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
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

    private Operation parseLine(String line) throws IllegalArgumentException {
        String[] splitLine = line.split(",");

        LocalDate date = LocalDate.parse(splitLine[0], DATE_TIME_FORMATTER);
        Stock stock = new Stock(splitLine[1]);
        BigDecimal amount = new BigDecimal(splitLine[2]);
        try {
            BigDecimal price = new BigDecimal(nf.parse(splitLine[3].replace(CURRENCY_SYMBOL_PREFIX, "")).toString(), CURRENCY_MATH_CONTEXT);
            BigDecimal tax1 = new BigDecimal(nf.parse(splitLine[4].replace(CURRENCY_SYMBOL_PREFIX, "")).toString(), CURRENCY_MATH_CONTEXT);
            BigDecimal tax2 = new BigDecimal(nf.parse(splitLine[5].replace(CURRENCY_SYMBOL_PREFIX, "")).toString(), CURRENCY_MATH_CONTEXT);
            return new Operation(date, stock, amount, price, tax1.add(tax2), OPERATION_TYPE);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid line format.", e);
        }
    }

}
