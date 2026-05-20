package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.ptax.PtaxService;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

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

    public CapitalGainsResult calculateCapitalGains(List<Operation> buys, List<Operation> sells, int year, PtaxService ptaxService) {
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        List<Operation> buysUpToYear = buys.stream()
                .filter(b -> !b.getDate().isAfter(endOfYear))
                .sorted(Comparator.comparing(Operation::getDate))
                .toList();

        Money yearGains = Money.zero(MoneyUtil.BRL);
        Money totalGains = Money.zero(MoneyUtil.BRL);
        BigDecimal totalSoldAmount = BigDecimal.ZERO;

        for (Operation sell : sells) {
            if (sell.getDate().isAfter(endOfYear)) continue;

            Money avgCostAtSellDate = avgCostBrlAtDate(buysUpToYear, sell.getDate(), ptaxService);
            Money sellBrl = toSellBrl(sell, ptaxService);
            Money costBrl = avgCostAtSellDate.multiply(sell.getAmount());
            Money gain = sellBrl.subtract(costBrl);

            totalGains = totalGains.add(gain);
            if (sell.getDate().getYear() == year) {
                yearGains = yearGains.add(gain);
            }

            totalSoldAmount = totalSoldAmount.add(sell.getAmount());
        }

        BigDecimal totalBuyAmount = buysUpToYear.stream()
                .map(Operation::getAmount)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        if (totalSoldAmount.compareTo(totalBuyAmount) > 0) {
            throw new IllegalStateException("Total sold (" + totalSoldAmount + ") exceeds total bought (" + totalBuyAmount + ")");
        }

        Money avgCostBrl = avgCostBrlAtDate(buysUpToYear, endOfYear, ptaxService);
        Money avgCostUsd = avgCostUsdAtDate(buysUpToYear, endOfYear);

        return new CapitalGainsResult(yearGains, totalGains, avgCostBrl, avgCostUsd);
    }

    private Money avgCostBrlAtDate(List<Operation> sortedBuys, LocalDate date, PtaxService ptaxService) {
        Money totalCost = Money.zero(MoneyUtil.BRL);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Operation buy : sortedBuys) {
            if (buy.getDate().isAfter(date)) break;
            totalCost = totalCost.add(toCostBrl(buy, ptaxService));
            totalAmount = totalAmount.add(buy.getAmount());
        }

        return totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalCost.divide(totalAmount)
                : Money.zero(MoneyUtil.BRL);
    }

    private Money avgCostUsdAtDate(List<Operation> sortedBuys, LocalDate date) {
        Money totalCost = Money.zero(MoneyUtil.USD);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Operation buy : sortedBuys) {
            if (buy.getDate().isAfter(date)) break;
            totalCost = totalCost.add(toCostUsd(buy));
            totalAmount = totalAmount.add(buy.getAmount());
        }

        return totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalCost.divide(totalAmount)
                : Money.zero(MoneyUtil.USD);
    }

    public DividendsResult calculateDividendsBrl(List<Dividend> dividends, PtaxService ptaxService) {
        Money grossBrl = Money.zero(MoneyUtil.BRL);
        Money taxBrl = Money.zero(MoneyUtil.BRL);

        for (Dividend d : dividends) {
            Money ptaxVenda = ptaxService.getCotacaoVenda(d.date());
            grossBrl = grossBrl.add(convertUsdToBrl(d.value(), ptaxVenda));
            taxBrl = taxBrl.add(convertUsdToBrl(d.tax(), ptaxVenda));
        }

        return new DividendsResult(grossBrl, taxBrl);
    }

    private Money toCostUsd(Operation buy) {
        return buy.getPrice().multiply(buy.getAmount()).add(buy.getTax());
    }

    private Money toCostBrl(Operation buy, PtaxService ptaxService) {
        Money costUsd = toCostUsd(buy);
        return convertUsdToBrl(costUsd, ptaxService.getCotacaoCompra(buy.getDate()));
    }

    private Money toSellBrl(Operation sell, PtaxService ptaxService) {
        Money sellUsd = sell.getPrice().multiply(sell.getAmount());
        return convertUsdToBrl(sellUsd, ptaxService.getCotacaoVenda(sell.getDate()));
    }

    private Money convertUsdToBrl(Money usdAmount, Money ptaxRate) {
        return Money.of(usdAmount.multiply(ptaxRate.getNumber()).getNumber(), MoneyUtil.BRL);
    }
}
