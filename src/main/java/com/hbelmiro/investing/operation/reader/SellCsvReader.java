package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
final class SellCsvReader extends CsvReader {

    @ConfigProperty(name = "operation.reader.csv.sell.path")
    String path;

    /**
     * Dummy constructor for CDI only.
     *
     * @deprecated CDI only.
     */
    @SuppressWarnings("unused")
    @Deprecated(forRemoval = true)
    private SellCsvReader() {
        super(null, null);
    }

    @Inject
    SellCsvReader(@ConfigProperty(name = "operation.reader.csv.sell.path") String path) {
        super(path, OperationType.SELL);
    }
}
