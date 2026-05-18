package com.hbelmiro.investing.ptax;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "bcb-ptax-api")
@Path("/")
public interface BcbPtaxRestClient {

    @GET
    @Path("CotacaoDolarDia%28dataCotacao=@dataCotacao%29")
    @Produces(MediaType.APPLICATION_JSON)
    BcbPtaxResponse getCotacaoDolarDia(
            @QueryParam("@dataCotacao") String dataCotacaoValue,
            @QueryParam("$format") String format
    );
}
