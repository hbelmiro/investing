package com.hbelmiro.investing.ptax.test;

import com.hbelmiro.investing.ptax.BcbPtaxResponse;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.ProcessingException;

@Vetoed
public class ThrowingStubBcbPtaxRestClient extends StubBcbPtaxRestClient {

    @Override
    public BcbPtaxResponse getCotacaoDolarDia(String dataCotacaoValue, String format) {
        throw new ProcessingException("Connection refused");
    }
}
