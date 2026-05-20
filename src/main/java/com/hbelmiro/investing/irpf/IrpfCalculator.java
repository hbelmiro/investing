package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.ptax.PtaxService;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        Money totalBuyCostBrl = Money.zero(MoneyUtil.BRL);
        Money totalBuyCostUsd = Money.zero(MoneyUtil.USD);
        BigDecimal totalBuyAmount = BigDecimal.ZERO;

        for (Operation buy : buys) {
            if (buy.getDate().isAfter(endOfYear)) continue;
            totalBuyCostBrl = totalBuyCostBrl.add(toCostBrl(buy, ptaxService));
            totalBuyCostUsd = totalBuyCostUsd.add(toCostUsd(buy));
            totalBuyAmount = totalBuyAmount.add(buy.getAmount());
        }

        Money avgCostBrl = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalBuyCostBrl.divide(totalBuyAmount)
                : Money.zero(MoneyUtil.BRL);

        Money avgCostUsd = totalBuyAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalBuyCostUsd.divide(totalBuyAmount)
                : Money.zero(MoneyUtil.USD);

        Money yearGains = Money.zero(MoneyUtil.BRL);
        Money totalGains = Money.zero(MoneyUtil.BRL);
        BigDecimal totalSoldAmount = BigDecimal.ZERO;

        for (Operation sell : sells) {
            if (sell.getDate().isAfter(endOfYear)) continue;
            Money sellBrl = toSellBrl(sell, ptaxService);
            Money costBrl = avgCostBrl.multiply(sell.getAmount());
            Money gain = sellBrl.subtract(costBrl);

            totalGains = totalGains.add(gain);
            if (sell.getDate().getYear() == year) {
                yearGains = yearGains.add(gain);
            }

            totalSoldAmount = totalSoldAmount.add(sell.getAmount());
        }

        if (totalSoldAmount.compareTo(totalBuyAmount) > 0) {
            throw new IllegalStateException("Total sold (" + totalSoldAmount + ") exceeds total bought (" + totalBuyAmount + ")");
        }

        return new CapitalGainsResult(yearGains, totalGains, avgCostBrl, avgCostUsd);
    }

    public Money calculateDividendsBrl(List<Dividend> dividends, PtaxService ptaxService) {
        if (dividends.isEmpty()) {
            return Money.zero(MoneyUtil.BRL);
        }

        Money total = Money.zero(MoneyUtil.BRL);
        for (Dividend d : dividends) {
            Money netUsd = d.value().subtract(d.tax());
            Money brlValue = convertUsdToBrl(netUsd, ptaxService.getCotacaoVenda(d.date()));
            total = total.add(brlValue);
        }
        return total;
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
