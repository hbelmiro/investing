package com.hbelmiro.investing;

import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;

@ApplicationScoped
public class FundamentusClient {

    public Indicators read(String ticker) {
        Document document;
        try {
            document = Jsoup.connect("https://www.fundamentus.com.br/detalhes.php?papel=" + ticker).get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Elements spans = document.select("span[class=txt]");
        Element dySpan = spans.stream().filter(e -> "Div. Yield".equals(e.html())).findAny().orElseThrow();
        String dyString = dySpan.parent().nextElementSibling().getElementsByClass("txt").html();

        Element lpaSpan = spans.stream().filter(e -> "LPA".equals(e.html())).findAny().orElseThrow();
        String lpaString = lpaSpan.parent().nextElementSibling().getElementsByClass("txt").html();

        Element netWorthSpan = spans.stream().filter(e -> "Patrim. Líq".equals(e.html())).findAny().orElseThrow();
        String netWorthString = netWorthSpan.parent().nextElementSibling().getElementsByClass("txt").html();

        Element sharesSpan = spans.stream().filter(e -> "Nro. Ações".equals(e.html())).findAny().orElseThrow();
        String sharesString = sharesSpan.parent().nextElementSibling().getElementsByClass("txt").html();

        BigDecimal dy = new BigDecimal(dyString.replace(',', '.').replace("%", "")).divide(new BigDecimal("100"));
        BigDecimal lpa = new BigDecimal(lpaString.replace(".", "").replace(',', '.'));
        BigDecimal netWorth = new BigDecimal(netWorthString.replace(".", "").replace(',', '.'));
        BigDecimal shares = new BigDecimal(sharesString.replace(".", "").replace(',', '.'));

        return new Indicators(dy, lpa, netWorth, shares);
    }
}
