package com.hbelmiro.investing.irpf;

import com.hbelmiro.investing.Operation;
import com.hbelmiro.investing.OperationType;
import com.hbelmiro.investing.dividend.Dividend;
import com.hbelmiro.investing.ptax.PtaxService;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public final class IrpfCalculator {

    IrpfCalculator() {
    }

    public Money calculateAverageCostBrl(List<Operation> buys, PtaxService ptaxService) {
        if (buys.isEmpty()) {
            return Money.zero(MoneyUtil.BRL);
        }

        Money totalCostBrl = Money.zero(MoneyUtil.BRL);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Operation buy : buys) {
            Money costBrl = toCostBrl(buy, ptaxService);
            totalCostBrl = totalCostBrl.add(costBrl);
            totalAmount = totalAmount.add(buy.getAmount());
        }

        return totalCostBrl.divide(totalAmount);
    }

    public Money calculateCapitalGains(List<Operation> buys, List<Operation> sells, int year, PtaxService ptaxService) {
        if (sells.isEmpty()) {
            return Money.zero(MoneyUtil.BRL);
        }

        Money totalCostBrl = Money.zero(MoneyUtil.BRL);
        BigDecimal totalAmount = BigDecimal.ZERO;
        Money totalGains = Money.zero(MoneyUtil.BRL);

        List<Operation> allOps = Stream.concat(buys.stream(), sells.stream())
                .sorted(Comparator.comparing(Operation::getDate))
                .toList();

        for (Operation op : allOps) {
            if (op.getType() == OperationType.BUY) {
                Money costBrl = toCostBrl(op, ptaxService);
                totalCostBrl = totalCostBrl.add(costBrl);
                totalAmount = totalAmount.add(op.getAmount());
            } else {
                if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("Sell without prior buy position on " + op.getDate());
                }
                if (op.getAmount().compareTo(totalAmount) > 0) {
                    throw new IllegalStateException("Sell amount exceeds current position on " + op.getDate());
                }
                Money runningAvgCostBrl = totalCostBrl.divide(totalAmount);
                BigDecimal sellPriceUsd = op.getPrice().getNumber().numberValue(BigDecimal.class);
                BigDecimal ptaxVenda = ptaxService.getCotacaoVenda(op.getDate()).getNumber().numberValue(BigDecimal.class);
                Money sellBrl = Money.of(sellPriceUsd.multiply(op.getAmount()).multiply(ptaxVenda), MoneyUtil.BRL);
                Money costBrl = runningAvgCostBrl.multiply(op.getAmount());
                if (op.getDate().getYear() == year) {
                    totalGains = totalGains.add(sellBrl.subtract(costBrl));
                }

                totalCostBrl = totalCostBrl.subtract(runningAvgCostBrl.multiply(op.getAmount()));
                totalAmount = totalAmount.subtract(op.getAmount());
            }
        }

        return totalGains;
    }

    public Money calculateDividendsBrl(List<Dividend> dividends, PtaxService ptaxService) {
        if (dividends.isEmpty()) {
            return Money.zero(MoneyUtil.BRL);
        }

        Money total = Money.zero(MoneyUtil.BRL);
        for (Dividend d : dividends) {
            BigDecimal netUsd = d.value().subtract(d.tax()).getNumber().numberValue(BigDecimal.class);
            BigDecimal ptaxVenda = ptaxService.getCotacaoVenda(d.date()).getNumber().numberValue(BigDecimal.class);
            total = total.add(Money.of(netUsd.multiply(ptaxVenda), MoneyUtil.BRL));
        }
        return total;
    }

    private Money toCostBrl(Operation buy, PtaxService ptaxService) {
        BigDecimal costUsd = buy.getPrice().getNumber().numberValue(BigDecimal.class)
                .multiply(buy.getAmount())
                .add(buy.getTax().getNumber().numberValue(BigDecimal.class));
        BigDecimal ptaxCompra = ptaxService.getCotacaoCompra(buy.getDate()).getNumber().numberValue(BigDecimal.class);
        return Money.of(costUsd.multiply(ptaxCompra), MoneyUtil.BRL);
    }
}
