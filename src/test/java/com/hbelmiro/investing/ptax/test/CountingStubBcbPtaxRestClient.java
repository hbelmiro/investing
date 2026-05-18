package com.hbelmiro.investing.ptax.test;

import com.hbelmiro.investing.ptax.BcbPtaxResponse;
import jakarta.enterprise.inject.Vetoed;

@Vetoed
public class CountingStubBcbPtaxRestClient extends StubBcbPtaxRestClient {
    public int callCount = 0;

    @Override
    public BcbPtaxResponse getCotacaoDolarDia(String dataCotacaoValue, String format) {
        callCount++;
        return super.getCotacaoDolarDia(dataCotacaoValue, format);
    }
}
