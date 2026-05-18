package com.hbelmiro.investing.ptax;

import com.hbelmiro.investing.ptax.test.CountingStubBcbPtaxRestClient;
import com.hbelmiro.investing.ptax.test.NullFirstResponseStubBcbPtaxRestClient;
import com.hbelmiro.investing.ptax.test.StubBcbPtaxRestClient;
import com.hbelmiro.investing.ptax.test.ThrowingStubBcbPtaxRestClient;
import com.hbelmiro.investing.utils.MoneyUtil;
import jakarta.ws.rs.ProcessingException;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DefaultPtaxServiceTest {

    @Test
    void getCotacaoCompraWeekdayReturnsRate() {
        var stub = new StubBcbPtaxRestClient();
        var rate = new PtaxRate(new BigDecimal("6.0370"), new BigDecimal("6.0380"), "2025-01-15 13:06:00.000");
        stub.addResponse("'01-15-2025'", new BcbPtaxResponse(List.of(rate)));

        var service = new DefaultPtaxService(stub);

        Money result = service.getCotacaoCompra(LocalDate.of(2025, 1, 15));
        assertThat(result).isEqualTo(Money.of(new BigDecimal("6.0370"), MoneyUtil.BRL));
    }

    @Test
    void getCotacaoVendaWeekdayReturnsRate() {
        var stub = new StubBcbPtaxRestClient();
        var rate = new PtaxRate(new BigDecimal("6.0370"), new BigDecimal("6.0380"), "2025-01-15 13:06:00.000");
        stub.addResponse("'01-15-2025'", new BcbPtaxResponse(List.of(rate)));

        var service = new DefaultPtaxService(stub);

        Money result = service.getCotacaoVenda(LocalDate.of(2025, 1, 15));
        assertThat(result).isEqualTo(Money.of(new BigDecimal("6.0380"), MoneyUtil.BRL));
    }

    @Test
    void getCotacaoCompraSaturdayFallsBackToFriday() {
        var stub = new StubBcbPtaxRestClient();
        var fridayRate = new PtaxRate(new BigDecimal("6.1000"), new BigDecimal("6.1010"), "2025-01-17 13:06:00.000");
        stub.addResponse("'01-17-2025'", new BcbPtaxResponse(List.of(fridayRate)));

        var service = new DefaultPtaxService(stub);

        Money result = service.getCotacaoCompra(LocalDate.of(2025, 1, 18));
        assertThat(result).isEqualTo(Money.of(new BigDecimal("6.1000"), MoneyUtil.BRL));
    }

    @Test
    void getCotacaoCompraSundayFallsBackToFriday() {
        var stub = new StubBcbPtaxRestClient();
        var fridayRate = new PtaxRate(new BigDecimal("6.1000"), new BigDecimal("6.1010"), "2025-01-17 13:06:00.000");
        stub.addResponse("'01-17-2025'", new BcbPtaxResponse(List.of(fridayRate)));

        var service = new DefaultPtaxService(stub);

        Money result = service.getCotacaoCompra(LocalDate.of(2025, 1, 19));
        assertThat(result).isEqualTo(Money.of(new BigDecimal("6.1000"), MoneyUtil.BRL));
    }

    @Test
    void getCotacaoCompraMultiDayHolidayFallsBackMultipleDays() {
        var stub = new StubBcbPtaxRestClient();
        var rate = new PtaxRate(new BigDecimal("5.9500"), new BigDecimal("5.9510"), "2024-12-30 13:06:00.000");
        stub.addResponse("'12-30-2024'", new BcbPtaxResponse(List.of(rate)));

        var service = new DefaultPtaxService(stub);

        Money result = service.getCotacaoCompra(LocalDate.of(2025, 1, 4));
        assertThat(result).isEqualTo(Money.of(new BigDecimal("5.9500"), MoneyUtil.BRL));
    }

    @Test
    void getCotacaoCompraNoRateWithin7DaysThrowsException() {
        var stub = new StubBcbPtaxRestClient();

        var service = new DefaultPtaxService(stub);

        assertThatExceptionOfType(PtaxRateNotFoundException.class)
                .isThrownBy(() -> service.getCotacaoCompra(LocalDate.of(2025, 1, 15)))
                .withMessageContaining("2025-01-15");
    }

    @Test
    void getCotacaoCompraCachedResultDoesNotCallRestClientAgain() {
        var stub = new CountingStubBcbPtaxRestClient();
        var rate = new PtaxRate(new BigDecimal("6.0370"), new BigDecimal("6.0380"), "2025-01-15 13:06:00.000");
        stub.addResponse("'01-15-2025'", new BcbPtaxResponse(List.of(rate)));

        var service = new DefaultPtaxService(stub);

        service.getCotacaoCompra(LocalDate.of(2025, 1, 15));
        service.getCotacaoCompra(LocalDate.of(2025, 1, 15));

        assertThat(stub.callCount).isEqualTo(1);
    }

    @Test
    void getCotacaoCompraWeekendAfterWeekdayCachedUsesCache() {
        var stub = new CountingStubBcbPtaxRestClient();
        var fridayRate = new PtaxRate(new BigDecimal("6.1000"), new BigDecimal("6.1010"), "2025-01-17 13:06:00.000");
        stub.addResponse("'01-17-2025'", new BcbPtaxResponse(List.of(fridayRate)));

        var service = new DefaultPtaxService(stub);

        service.getCotacaoCompra(LocalDate.of(2025, 1, 17));
        Money result = service.getCotacaoCompra(LocalDate.of(2025, 1, 18));

        assertThat(result).isEqualTo(Money.of(new BigDecimal("6.1000"), MoneyUtil.BRL));
        assertThat(stub.callCount).isEqualTo(1);
    }

    @Test
    void getCotacaoCompraMultipleIntradayRatesReturnsLastRate() {
        var stub = new StubBcbPtaxRestClient();
        var morningRate = new PtaxRate(new BigDecimal("6.0100"), new BigDecimal("6.0110"), "2025-01-15 10:00:00.000");
        var closingRate = new PtaxRate(new BigDecimal("6.0370"), new BigDecimal("6.0380"), "2025-01-15 13:06:00.000");
        stub.addResponse("'01-15-2025'", new BcbPtaxResponse(List.of(morningRate, closingRate)));

        var service = new DefaultPtaxService(stub);

        Money result = service.getCotacaoCompra(LocalDate.of(2025, 1, 15));
        assertThat(result).isEqualTo(Money.of(new BigDecimal("6.0370"), MoneyUtil.BRL));
    }

    @Test
    void getCotacaoCompraRateOnLastLookbackDayReturnsRate() {
        var stub = new StubBcbPtaxRestClient();
        var rate = new PtaxRate(new BigDecimal("6.2000"), new BigDecimal("6.2010"), "2025-01-09 13:06:00.000");
        stub.addResponse("'01-09-2025'", new BcbPtaxResponse(List.of(rate)));

        var service = new DefaultPtaxService(stub);

        Money result = service.getCotacaoCompra(LocalDate.of(2025, 1, 15));
        assertThat(result).isEqualTo(Money.of(new BigDecimal("6.2000"), MoneyUtil.BRL));
    }

    @Test
    void getCotacaoCompraRateOnlyBeyondLookbackWindowThrowsException() {
        var stub = new StubBcbPtaxRestClient();
        var rate = new PtaxRate(new BigDecimal("6.2000"), new BigDecimal("6.2010"), "2025-01-08 13:06:00.000");
        stub.addResponse("'01-08-2025'", new BcbPtaxResponse(List.of(rate)));

        var service = new DefaultPtaxService(stub);

        assertThatExceptionOfType(PtaxRateNotFoundException.class)
                .isThrownBy(() -> service.getCotacaoCompra(LocalDate.of(2025, 1, 15)));
    }

    @Test
    void getCotacaoCompraRestClientThrowsExceptionPropagates() {
        var stub = new ThrowingStubBcbPtaxRestClient();

        var service = new DefaultPtaxService(stub);

        assertThatExceptionOfType(ProcessingException.class)
                .isThrownBy(() -> service.getCotacaoCompra(LocalDate.of(2025, 1, 15)))
                .withMessageContaining("Connection refused");
    }

    @Test
    void getCotacaoCompraNullValueInResponseFallsBack() {
        var stub = new NullFirstResponseStubBcbPtaxRestClient();
        var rate = new PtaxRate(new BigDecimal("6.0370"), new BigDecimal("6.0380"), "2025-01-14 13:06:00.000");
        stub.addResponse("'01-14-2025'", new BcbPtaxResponse(List.of(rate)));

        var service = new DefaultPtaxService(stub);

        Money result = service.getCotacaoCompra(LocalDate.of(2025, 1, 15));
        assertThat(result).isEqualTo(Money.of(new BigDecimal("6.0370"), MoneyUtil.BRL));
    }
}
