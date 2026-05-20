package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.dividend.UsDividendReader;
import com.hbelmiro.investing.operation.reader.UsFixedIncomeBuyReader;
import com.hbelmiro.investing.operation.reader.UsFixedIncomeSellReader;
import com.hbelmiro.investing.operation.reader.UsReitsBuyReader;
import com.hbelmiro.investing.operation.reader.UsReitsSellReader;
import com.hbelmiro.investing.operation.reader.UsStocksBuyReader;
import com.hbelmiro.investing.operation.reader.UsStocksSellReader;
import com.hbelmiro.investing.ptax.PtaxService;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.javamoney.moneta.Money;

import javax.money.MonetaryRounding;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/api/irpf")
public class IrpfResource {

    @Inject
    UsStocksBuyReader usStocksBuyReader;

    @Inject
    UsFixedIncomeBuyReader usFixedIncomeBuyReader;

    @Inject
    UsReitsBuyReader usReitsBuyReader;

    @Inject
    UsStocksSellReader usStocksSellReader;

    @Inject
    UsFixedIncomeSellReader usFixedIncomeSellReader;

    @Inject
    UsReitsSellReader usReitsSellReader;

    @Inject
    UsDividendReader usDividendReader;

    @Inject
    IrpfCalculator irpfCalculator;

    @Inject
    PtaxService ptaxService;

    @GET
    @Path("years")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> getAvailableYears() {
        List<Operation> allBuys = new ArrayList<>();
        allBuys.addAll(usStocksBuyReader.read());
        allBuys.addAll(usFixedIncomeBuyReader.read());
        allBuys.addAll(usReitsBuyReader.read());

        int firstYear = allBuys.stream()
                .mapToInt(op -> op.getDate().getYear())
                .min()
                .orElse(LocalDate.now().getYear());

        int currentYear = LocalDate.now().getYear();
        firstYear = Math.min(firstYear, currentYear);
        List<Integer> years = new ArrayList<>();
        for (int y = firstYear; y <= currentYear; y++) {
            years.add(y);
        }
        return years;
    }

    @GET
    @Path("us_stocks")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IrpfAssetData> getUsStocksIrpf(@QueryParam("year") int year) {
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        MonetaryRounding rounding = Monetary.getDefaultRounding();

        List<Operation> allBuys = new ArrayList<>();
        allBuys.addAll(usStocksBuyReader.read());
        allBuys.addAll(usFixedIncomeBuyReader.read());
        allBuys.addAll(usReitsBuyReader.read());

        List<Operation> allSellsRaw = new ArrayList<>();
        allSellsRaw.addAll(usStocksSellReader.read());
        allSellsRaw.addAll(usFixedIncomeSellReader.read());
        allSellsRaw.addAll(usReitsSellReader.read());
        List<Operation> allSells = allSellsRaw.stream().filter(op -> isValidSymbol(op.getAsset().symbol())).toList();

        List<Dividend> allDividends = usDividendReader.read().stream()
                .filter(d -> isValidSymbol(d.asset().symbol()))
                .toList();

        Set<String> symbols = allBuys.stream()
                .filter(op -> !op.getDate().isAfter(endOfYear))
                .map(op -> op.getAsset().symbol())
                .collect(Collectors.toSet());

        validateSymbolsHaveBuys(symbols, allSells, allDividends, year);

        return symbols.stream().sorted().map(symbol -> {
            try {
                List<Operation> symbolBuys = allBuys.stream()
                        .filter(op -> op.getAsset().symbol().equals(symbol))
                        .toList();

                List<Operation> symbolSells = allSells.stream()
                        .filter(op -> op.getAsset().symbol().equals(symbol))
                        .toList();

                List<Dividend> dividends = allDividends.stream()
                        .filter(d -> d.asset().symbol().equals(symbol))
                        .filter(d -> d.date().getYear() == year)
                        .toList();

                CapitalGainsResult gainsResult = irpfCalculator.calculateCapitalGains(symbolBuys, symbolSells, year, ptaxService);
                Money avgCostBrl = gainsResult.avgCostBrl().with(rounding);
                Money avgCostUsd = gainsResult.avgCostUsd().with(rounding);
                Money capitalGainsBrl = gainsResult.capitalGainsBrl().with(rounding);
                Money totalCapitalGainsBrl = gainsResult.totalCapitalGainsBrl().with(rounding);
                DividendsResult dividendsResult = irpfCalculator.calculateDividendsBrl(dividends, ptaxService);
                Money dividendsGrossBrl = dividendsResult.grossBrl().with(rounding);
                Money dividendsTaxBrl = dividendsResult.taxBrl().with(rounding);

                BigDecimal totalBought = symbolBuys.stream()
                        .filter(op -> !op.getDate().isAfter(endOfYear))
                        .map(Operation::getAmount)
                        .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
                BigDecimal totalSold = symbolSells.stream()
                        .filter(op -> !op.getDate().isAfter(endOfYear))
                        .map(Operation::getAmount)
                        .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
                BigDecimal quantity = totalBought.subtract(totalSold)
                        .setScale(5, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros();

                if (quantity.compareTo(BigDecimal.ZERO) == 0
                        && capitalGainsBrl.isZero() && dividends.isEmpty()) {
                    return null;
                }

                Money totalCostBrl = avgCostBrl.multiply(quantity).with(rounding);
                Money totalCostUsd = avgCostUsd.multiply(quantity).with(rounding);

                Money ptaxRate = avgCostUsd.isPositive()
                        ? avgCostBrl.divide(avgCostUsd.getNumber())
                        : Money.zero(MoneyUtil.BRL);

                return new IrpfAssetData(symbol, quantity, avgCostBrl, totalCostBrl, avgCostUsd, totalCostUsd, ptaxRate, capitalGainsBrl, totalCapitalGainsBrl, dividendsGrossBrl, dividendsTaxBrl, null);
            } catch (RuntimeException e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
                return IrpfAssetData.error(symbol, errorMessage);
            }
        }).filter(Objects::nonNull).toList();
    }

    private void validateSymbolsHaveBuys(Set<String> buySymbols, List<Operation> allSells,
                                         List<Dividend> allDividends, int year) {
        Set<String> sellSymbols = allSells.stream()
                .filter(op -> op.getDate().getYear() == year)
                .map(op -> op.getAsset().symbol())
                .collect(Collectors.toSet());

        Set<String> dividendSymbols = allDividends.stream()
                .filter(d -> d.date().getYear() == year)
                .map(d -> d.asset().symbol())
                .collect(Collectors.toSet());

        sellSymbols.removeAll(buySymbols);
        dividendSymbols.removeAll(buySymbols);

        if (!sellSymbols.isEmpty()) {
            throw new IllegalStateException("Sells for symbols without buy history: " + sellSymbols);
        }
        if (!dividendSymbols.isEmpty()) {
            throw new IllegalStateException("Dividends for symbols without buy history: " + dividendSymbols);
        }
    }

    private static boolean isValidSymbol(String symbol) {
        return symbol != null && !symbol.isBlank() && !symbol.contains(" ") && !"-".equals(symbol) && !"?".equals(symbol);
    }
}
