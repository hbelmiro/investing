package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.dividend.UsDividendReader;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/irpf")
public class IrpfResource {

    @Inject
    UsStocksBuyReader usStocksBuyReader;

    @Inject
    UsStocksSellReader usStocksSellReader;

    @Inject
    UsDividendReader usDividendReader;

    @Inject
    IrpfCalculator irpfCalculator;

    @Inject
    PtaxService ptaxService;

    @GET
    @Path("us_stocks")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IrpfAssetData> getUsStocksIrpf(@QueryParam("year") int year) {
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        MonetaryRounding rounding = Monetary.getDefaultRounding();

        List<Operation> allBuys = usStocksBuyReader.read();
        List<Operation> allSells = usStocksSellReader.read();
        List<Dividend> allDividends = usDividendReader.read();

        List<Operation> buysUpToYear = allBuys.stream()
                .filter(op -> !op.getDate().isAfter(endOfYear))
                .toList();

        Set<String> symbols = buysUpToYear.stream()
                .map(op -> op.getAsset().symbol())
                .collect(Collectors.toSet());

        validateSymbolsHaveBuys(symbols, allSells, allDividends, year);

        return symbols.stream().sorted().map(symbol -> {
            List<Operation> buys = buysUpToYear.stream()
                    .filter(op -> op.getAsset().symbol().equals(symbol))
                    .toList();

            List<Operation> yearSells = allSells.stream()
                    .filter(op -> op.getAsset().symbol().equals(symbol))
                    .filter(op -> op.getDate().getYear() == year)
                    .toList();

            List<Operation> allSymbolSells = allSells.stream()
                    .filter(op -> op.getAsset().symbol().equals(symbol))
                    .filter(op -> !op.getDate().isAfter(endOfYear))
                    .toList();

            List<Dividend> dividends = allDividends.stream()
                    .filter(d -> d.asset().symbol().equals(symbol))
                    .filter(d -> d.date().getYear() == year)
                    .toList();

            BigDecimal totalBought = buys.stream()
                    .map(Operation::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            BigDecimal totalSold = allSymbolSells.stream()
                    .map(Operation::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            BigDecimal quantity = totalBought.subtract(totalSold);

            if (quantity.compareTo(BigDecimal.ZERO) == 0 && yearSells.isEmpty() && dividends.isEmpty()) {
                return null;
            }

            Money avgCostBrl = irpfCalculator.calculateAverageCostBrl(buys, ptaxService).with(rounding);
            Money capitalGainsBrl = irpfCalculator.calculateCapitalGains(buys, allSymbolSells, year, ptaxService).with(rounding);
            Money dividendsBrl = irpfCalculator.calculateDividendsBrl(dividends, ptaxService).with(rounding);
            Money totalCostBrl = avgCostBrl.multiply(quantity).with(rounding);

            return new IrpfAssetData(
                    symbol,
                    quantity,
                    toBigDecimal(avgCostBrl),
                    toBigDecimal(totalCostBrl),
                    toBigDecimal(capitalGainsBrl),
                    toBigDecimal(dividendsBrl)
            );
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

    private static BigDecimal toBigDecimal(Money money) {
        return money.getNumber().numberValue(BigDecimal.class);
    }
}
