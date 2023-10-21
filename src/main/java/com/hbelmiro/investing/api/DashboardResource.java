package com.hbelmiro.investing.api;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.asset.Asset;
import com.hbelmiro.investing.currency.CurrencyCode;
import com.hbelmiro.investing.dividend.BrDividendReader;
import com.hbelmiro.investing.dividend.DividendReader;
import com.hbelmiro.investing.dividend.UsDividendReader;
import com.hbelmiro.investing.operation.averageprice.AveragePriceCalculator;
import com.hbelmiro.investing.operation.reader.BrStocksBuyReader;
import com.hbelmiro.investing.operation.reader.BrStocksSellReader;
import com.hbelmiro.investing.operation.reader.UsStocksBuyReader;
import com.hbelmiro.investing.operation.reader.UsStocksSellReader;
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

@Path("/")
public class DashboardResource {

    @Inject
    PriceReader priceReader;

    @Inject
    BrStocksBuyReader brStocksBuyReader;

    @Inject
    BrStocksSellReader brStocksSellReader;

    @Inject
    UsStocksBuyReader usStocksBuyReader;

    @Inject
    UsStocksSellReader usStocksSellReader;

    @Inject
    AveragePriceCalculator averagePriceCalculator;

    @Inject
    BrDividendReader brDividendReader;

    @Inject
    UsDividendReader usDividendReader;

    @GET
    @Path("brazil_stocks")
    @Produces(MediaType.APPLICATION_JSON)
    public String brazilStocks() {
        return getDashboardData(brStocksBuyReader.read(), brStocksSellReader.read());
    }

    @GET
    @Path("us_stocks")
    @Produces(MediaType.APPLICATION_JSON)
    public String usStocks() {
        return getDashboardData(usStocksBuyReader.read(), usStocksSellReader.read());
    }

    private String getDashboardData(List<Operation> buyOperations, List<Operation> sellOperations) {
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
    }

    private Optional<Row> createRow(Asset asset, List<Operation> buyOperations, List<Operation> sellOperations) {
        List<Operation> assetBuyOperations = buyOperations.stream()
                .filter(op -> op.getAsset().equals(asset)).toList();

        List<Operation> assetSellOperations = sellOperations.stream()
                .filter(op -> op.getAsset().equals(asset)).toList();

        BigDecimal bought = assetBuyOperations.stream()
                .map(Operation::getAmount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal sold = assetSellOperations.stream()
                .map(Operation::getAmount)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        BigDecimal amount = bought.subtract(sold).setScale(5, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        } else {
            List<Operation> assetOperations = Stream.concat(assetBuyOperations.stream(), assetSellOperations.stream())
                    .toList();

            Money averagePrice = averagePriceCalculator.calculate(assetOperations);
            Money currentPrice = priceReader.read(asset);

            return Optional.of(new Row(
                    asset.symbol(),
                    amount,
                    averagePrice,
                    currentPrice,
                    getProfit(currentPrice, averagePrice),
                    getDividends(asset)
            ));
        }
    }

    private static BigDecimal getProfit(Money currentPrice, Money averagePrice) {
        return currentPrice.getNumber().numberValue(BigDecimal.class)
                .subtract(averagePrice.getNumber().numberValue(BigDecimal.class))
                .divide(averagePrice.getNumber().numberValue(BigDecimal.class), 4, RoundingMode.HALF_UP);
    }

    private Money getDividends(Asset asset) {
        return getDividendReader(asset).read().stream()
                .filter(dividend -> dividend.asset().equals(asset))
                .map(dividend -> dividend.value().subtract(dividend.tax()).getNumber().numberValue(BigDecimal.class))
                .reduce(BigDecimal::add)
                .map(dividend -> Money.of(dividend, asset.currencyUnit()))
                .orElse(Money.zero(asset.currencyUnit()));
    }

    private DividendReader getDividendReader(Asset asset) {
        return switch (asset.currencyUnit().getCurrencyCode()) {
            case CurrencyCode.BRL -> brDividendReader;
            case CurrencyCode.USD -> usDividendReader;
            default -> throw new UnsupportedOperationException(
                    "Unsupported currency code: " + asset.currencyUnit().getCurrencyCode());
        };
    }

    private record Row(
            String asset,
            BigDecimal amount,
            Money averagePrice,
            Money currentPrice,
            BigDecimal profit,
            Money dividends) {

        private static final Locale PT_BR = Locale.of("pt", "BR");

        private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(PT_BR);

        private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);

        private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(PT_BR);

        static {
            PERCENT_FORMAT.setMaximumFractionDigits(2);
            NUMBER_FORMAT.setMaximumFractionDigits(5);
        }

        @Override
        public String toString() {
            return MessageFormat.format("[\"{0}\",\"{1}\",\"{2}\",\"{3}\",\"{4}\",\"{5}\"]",
                    asset,
                    NUMBER_FORMAT.format(amount),
                    MONEY_FORMAT.format(averagePrice.getNumber()),
                    MONEY_FORMAT.format(currentPrice.getNumber()),
                    PERCENT_FORMAT.format(profit),
                    MONEY_FORMAT.format(dividends.getNumber())
            );
        }
    }
}
