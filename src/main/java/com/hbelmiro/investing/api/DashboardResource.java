package com.hbelmiro.investing.api;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.operation.averageprice.AveragePriceCalculator;
import com.hbelmiro.investing.operation.reader.BuyReader;
import com.hbelmiro.investing.operation.reader.SellReader;
import com.hbelmiro.investing.price.PriceReader;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/hello_dash")
public class DashboardResource {

    @Inject
    PriceReader priceReader;

    @Inject
    BuyReader buyReader;

    @Inject
    SellReader sellReader;

    @Inject
    AveragePriceCalculator averagePriceCalculator;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String hello() {
        List<Operation> buyOperations = buyReader.read();
        List<Operation> sellOperations = sellReader.read();

        List<Asset> assets = buyOperations.stream()
                .map(Operation::getAsset)
                .distinct()
                .toList();

        return assets.stream()
                .map(asset -> createRow(asset, buyOperations, sellOperations))
                .filter(Optional::isPresent)
                .map(row -> row.map(Row::toString).orElseThrow())
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));


//        return """
//                    [
//                        ["John", "Paul"],
//                        ["Jane", "June"],
//                        ["Bob", "Rob"],
//                    ]
//                """;
    }

    private Optional<Row> createRow(Asset asset, List<Operation> buyOperations, List<Operation> sellOperations) {
        List<Operation> assetBuyOperations = buyOperations.stream()
                .filter(op -> op.getAsset().equals(asset)).toList();

        List<Operation> assetSellOperations = sellOperations.stream()
                .filter(op -> op.getAsset().equals(asset)).toList();

        List<Operation> assetOperations = Stream.concat(assetBuyOperations.stream(), assetSellOperations.stream())
                .toList();

        BigDecimal bought = assetBuyOperations.stream()
                .map(Operation::getAmount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal sold = assetSellOperations.stream()
                .map(Operation::getAmount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal amount = bought.subtract(sold);

        if (amount.equals(BigDecimal.ZERO)) {
            return Optional.empty();
        } else {
            Money averagePrice = averagePriceCalculator.calculate(assetOperations);

            Money currentPrice = priceReader.read(asset);

            BigDecimal profit = currentPrice.getNumber().numberValue(BigDecimal.class)
                    .subtract(averagePrice.getNumber().numberValue(BigDecimal.class))
                    .divide(averagePrice.getNumber().numberValue(BigDecimal.class), 4, RoundingMode.HALF_UP);

            return Optional.of(new Row(asset.symbol(), amount, averagePrice, currentPrice, profit));
        }
    }

    private record Row(String asset, BigDecimal amount, Money averagePrice, Money currentPrice, BigDecimal profit) {

        private static final Locale PT_BR = Locale.of("pt", "BR");

        private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(PT_BR);

        private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);

        private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(PT_BR);

        static {
            PERCENT_FORMAT.setMaximumFractionDigits(2);
        }

        @Override
        public String toString() {
            return MessageFormat.format("[\"{0}\",\"{1}\",\"{2}\",\"{3}\",\"{4}\"]",
                    asset,
                    NUMBER_FORMAT.format(amount),
                    MONEY_FORMAT.format(averagePrice.getNumber()),
                    MONEY_FORMAT.format(currentPrice.getNumber()),
                    PERCENT_FORMAT.format(profit)
            );
        }
    }
}
