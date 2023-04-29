package com.hbelmiro.investing.operation.reader;

import com.hbelmiro.investing.OperationType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
final class BuyCsvReader extends CsvReader {

    /**
     * Dummy constructor for CDI only.
     *
     * @deprecated CDI only.
     */
    @SuppressWarnings("unused")
    @Deprecated(forRemoval = true)
    private BuyCsvReader() {
        super(null, null);
    }

    @Inject
    BuyCsvReader(@ConfigProperty(name = "operation.reader.csv.buy.path") String path) {
        super(path, OperationType.BUY);
    }
}
