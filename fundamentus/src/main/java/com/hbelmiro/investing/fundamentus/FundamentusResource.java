package com.hbelmiro.investing.fundamentus;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/hello")
public class FundamentusResource {

    private final FundamentusClient fundamentusClient;

    @Inject
    public FundamentusResource(FundamentusClient fundamentusClient) {
        this.fundamentusClient = fundamentusClient;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws IOException {
        return fundamentusClient.read("ITUB3").toString();
    }
}
