package com.hbelmiro.investing.ptax.test;

import com.hbelmiro.investing.ptax.BcbPtaxResponse;
import com.hbelmiro.investing.ptax.BcbPtaxRestClient;

import jakarta.enterprise.inject.Vetoed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Vetoed
public class StubBcbPtaxRestClient implements BcbPtaxRestClient {
    private final Map<String, BcbPtaxResponse> responses = new HashMap<>();

    public void addResponse(String dataCotacao, BcbPtaxResponse response) {
        responses.put(dataCotacao, response);
    }

    @Override
    public BcbPtaxResponse getCotacaoDolarDia(String dataCotacaoValue, String format) {
        return responses.getOrDefault(dataCotacaoValue, new BcbPtaxResponse(List.of()));
    }
}
