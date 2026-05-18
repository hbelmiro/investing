package com.hbelmiro.investing.ptax;

import com.hbelmiro.investing.utils.MoneyUtil;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.javamoney.moneta.Money;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@UnlessBuildProfile("test")
public class DefaultPtaxService implements PtaxService {

    static final int MAX_LOOKBACK_DAYS = 7;
    static final int MAX_CACHE_SIZE = 1000;
    private static final DateTimeFormatter BCB_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String API_RESPONSE_FORMAT = "json";

    private final BcbPtaxRestClient restClient;
    private final Map<LocalDate, PtaxRate> cache = new ConcurrentHashMap<>();

    @Inject
    DefaultPtaxService(@RestClient BcbPtaxRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Money getCotacaoCompra(LocalDate date) {
        PtaxRate rate = getRate(date);
        return Money.of(rate.cotacaoCompra(), MoneyUtil.BRL);
    }

    @Override
    public Money getCotacaoVenda(LocalDate date) {
        PtaxRate rate = getRate(date);
        return Money.of(rate.cotacaoVenda(), MoneyUtil.BRL);
    }

    PtaxRate getRate(LocalDate date) {
        for (int i = 0; i < MAX_LOOKBACK_DAYS; i++) {
            LocalDate lookupDate = date.minusDays(i);
            PtaxRate cachedRate = cache.get(lookupDate);
            if (cachedRate != null) {
                if (!date.equals(lookupDate)) {
                    cache.put(date, cachedRate);
                }
                return cachedRate;
            }
        }

        for (int i = 0; i < MAX_LOOKBACK_DAYS; i++) {
            LocalDate lookupDate = date.minusDays(i);
            String formattedDate = "'" + lookupDate.format(BCB_DATE_FORMAT) + "'";
            BcbPtaxResponse response = restClient.getCotacaoDolarDia(formattedDate, API_RESPONSE_FORMAT);
            List<PtaxRate> rates = response.value();
            if (rates != null && !rates.isEmpty()) {
                PtaxRate rate = rates.getLast();
                evictIfNeeded();
                cache.put(lookupDate, rate);
                if (!date.equals(lookupDate)) {
                    cache.put(date, rate);
                }
                return rate;
            }
        }
        throw new PtaxRateNotFoundException(date, MAX_LOOKBACK_DAYS);
    }

    private void evictIfNeeded() {
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.keySet().stream().min(LocalDate::compareTo).ifPresent(cache::remove);
        }
    }
}
