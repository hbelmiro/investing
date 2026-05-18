package com.hbelmiro.investing.ptax.test;

import com.hbelmiro.investing.ptax.BcbPtaxResponse;
import jakarta.enterprise.inject.Vetoed;

@Vetoed
public class NullFirstResponseStubBcbPtaxRestClient extends StubBcbPtaxRestClient {

    private int callCount = 0;

    @Override
    public BcbPtaxResponse getCotacaoDolarDia(String dataCotacaoValue, String format) {
        callCount++;
        if (callCount == 1) {
            return new BcbPtaxResponse(null);
        }
        return super.getCotacaoDolarDia(dataCotacaoValue, format);
    }
}
