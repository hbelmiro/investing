package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * IRPF tax calculations per Receita Federal rules.
 *
 * @see <a href="https://www.gov.br/receitafederal/pt-br/assuntos/meu-imposto-de-renda/pagamento/renda-variavel/bolsa-de-valores-1/ganho-liquido">Ganho Líquido — Receita Federal</a>
 */
@ApplicationScoped
public final class IrpfCalculator {

    IrpfCalculator() {
    }

    public CapitalGainsResult calculateCapitalGains(List<Operation> buys, List<Operation> sells, int year, CurrencyConverter converter) {
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        List<Operation> buysUpToYear = buys.stream()
                .filter(b -> !b.getDate().isAfter(endOfYear))
                .sorted(Comparator.comparing(Operation::getDate))
                .toList();

        List<Operation> sellsUpToYear = sells.stream()
                .filter(s -> !s.getDate().isAfter(endOfYear))
                .sorted(Comparator.comparing(Operation::getDate))
                .toList();

        Money yearGains = Money.zero(MoneyUtil.BRL);
        Money totalGains = Money.zero(MoneyUtil.BRL);
        BigDecimal cumulativeSold = BigDecimal.ZERO;

        for (Operation sell : sellsUpToYear) {
            cumulativeSold = cumulativeSold.add(sell.getAmount());

            BigDecimal cumulativeBought = cumulativeBoughtAtDate(buysUpToYear, sell.getDate());
            if (cumulativeSold.compareTo(cumulativeBought) > 0) {
                throw new IllegalStateException(
                        "Sell amount exceeds position on " + sell.getDate()
                        + " (sold: " + cumulativeSold + ", bought: " + cumulativeBought + ")");
            }

            Money avgCostAtSellDate = avgCostBrlAtDate(buysUpToYear, sell.getDate(), converter);
            Money sellBrl = converter.toSellBrl(sell);
            Money costBrl = avgCostAtSellDate.multiply(sell.getAmount());
            Money gain = sellBrl.subtract(costBrl);

            totalGains = totalGains.add(gain);
            if (sell.getDate().getYear() == year) {
                yearGains = yearGains.add(gain);
            }
        }

        Money avgCostBrl = avgCostBrlAtDate(buysUpToYear, endOfYear, converter);
        Money avgCostOriginal = avgCostOriginalAtDate(buysUpToYear, endOfYear);

        return new CapitalGainsResult(yearGains, totalGains, avgCostBrl, avgCostOriginal);
    }

    private BigDecimal cumulativeBoughtAtDate(List<Operation> sortedBuys, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (Operation buy : sortedBuys) {
            if (buy.getDate().isAfter(date)) break;
            total = total.add(buy.getAmount());
        }
        return total;
    }

    private Money avgCostBrlAtDate(List<Operation> sortedBuys, LocalDate date, CurrencyConverter converter) {
        Money totalCost = Money.zero(MoneyUtil.BRL);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Operation buy : sortedBuys) {
            if (buy.getDate().isAfter(date)) break;
            totalCost = totalCost.add(converter.toCostBrl(buy));
            totalAmount = totalAmount.add(buy.getAmount());
        }

        return totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalCost.divide(totalAmount)
                : Money.zero(MoneyUtil.BRL);
    }

    private Money avgCostOriginalAtDate(List<Operation> sortedBuys, LocalDate date) {
        if (sortedBuys.isEmpty()) {
            return Money.zero(MoneyUtil.BRL);
        }

        CurrencyUnit currency = sortedBuys.getFirst().getPrice().getCurrency();
        Money totalCost = Money.zero(currency);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Operation buy : sortedBuys) {
            if (buy.getDate().isAfter(date)) break;
            totalCost = totalCost.add(rawCost(buy));
            totalAmount = totalAmount.add(buy.getAmount());
        }

        return totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalCost.divide(totalAmount)
                : Money.zero(currency);
    }

    public DividendsResult calculateDividendsBrl(List<Dividend> dividends, CurrencyConverter converter) {
        Money grossBrl = Money.zero(MoneyUtil.BRL);
        Money taxBrl = Money.zero(MoneyUtil.BRL);

        for (Dividend d : dividends) {
            grossBrl = grossBrl.add(converter.toDividendGrossBrl(d));
            taxBrl = taxBrl.add(converter.toDividendTaxBrl(d));
        }

        return new DividendsResult(grossBrl, taxBrl);
    }

    private Money rawCost(Operation buy) {
        return buy.getPrice().multiply(buy.getAmount()).add(buy.getTax());
    }
}
