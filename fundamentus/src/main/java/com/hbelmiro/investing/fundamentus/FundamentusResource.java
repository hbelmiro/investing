package com.hbelmiro.investing.fundamentus;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/hello")
public class FundamentusResource {

    private static final NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("pt", "BR"));

    private static final List<String> tickers = List.of(
            "GRND3",
            "ITUB3",
            "EGIE3",
            "ENBR3",
            "TAEE11",
            "FLRY3",
            "WEGE3",
            "LREN3",
            "MDIA3",
            "BIDI11",
            "INBR31",
            "BBDC3",
            "BBAS3",
            "BRSR6",
            "EZTC3",
            "MGLU3",
            "PSSA3",
            "ODPV3",
            "XPBR31",
            "ARZZ3",
            "HYPE3",
            "BBSE3"
    );

    private final FundamentusClient fundamentusClient;

    @Inject
    public FundamentusResource(FundamentusClient fundamentusClient) {
        this.fundamentusClient = fundamentusClient;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        Map<String, Optional<Indicators>> map = new LinkedHashMap<>(tickers.size());

        tickers.forEach(ticker -> {
            Optional<Indicators> indicators;
            try {
                indicators = Optional.of(fundamentusClient.read(ticker));
            } catch (Exception e) {
                indicators = Optional.empty();
            }
            map.put(ticker, indicators);
        });

        String lpa = map.values().stream()
                             .map(indicators -> indicators.map(Indicators::lpa).map(currencyFormatter::format).orElse("Error"))
                             .collect(Collectors.joining(System.lineSeparator()));

        String dy = map.values().stream()
                            .map(indicators -> indicators.map(Indicators::dividendYield).map(currencyFormatter::format).orElse("Error"))
                            .collect(Collectors.joining(System.lineSeparator()));

        String netWorth = map.values().stream()
                             .map(indicators -> indicators.map(Indicators::netWorth).map(currencyFormatter::format).orElse("Error"))
                             .collect(Collectors.joining(System.lineSeparator()));

        String shares = map.values().stream()
                                  .map(indicators -> indicators.map(Indicators::shares).map(currencyFormatter::format).orElse("Error"))
                                  .collect(Collectors.joining(System.lineSeparator()));

        return """
                LPA
                ===
                """ + lpa + """
                
                
                Patrimônio Líquido
                ==================
                """ + netWorth + """
                               
                               
                NÚmero Ações
                ============
                """ + shares + """
                               
                               
                DY
                ==
                """ + dy;
    }
}
