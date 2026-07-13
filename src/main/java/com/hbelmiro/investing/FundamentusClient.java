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

        String dyString = readIndicatorValue(spans, "Div. Yield");
        String lpaString = readIndicatorValue(spans, "LPA");
        String netWorthString = readIndicatorValue(spans, "Patrim. Líq");
        String sharesString = readIndicatorValue(spans, "Nro. Ações");

        BigDecimal dy = new BigDecimal(dyString.replace(',', '.').replace("%", "")).divide(new BigDecimal("100"));
        BigDecimal lpa = new BigDecimal(lpaString.replace(".", "").replace(',', '.'));
        BigDecimal netWorth = new BigDecimal(netWorthString.replace(".", "").replace(',', '.'));
        BigDecimal shares = new BigDecimal(sharesString.replace(".", "").replace(',', '.'));

        return new Indicators(dy, lpa, netWorth, shares);
    }

    private static String readIndicatorValue(Elements spans, String label) {
        Element span = spans.stream().filter(e -> label.equals(e.html())).findAny().orElseThrow();
        Element parent = span.parent();
        if (parent == null) {
            throw new IllegalStateException("No parent element found for indicator: " + label);
        }
        Element sibling = parent.nextElementSibling();
        if (sibling == null) {
            throw new IllegalStateException("No sibling element found for indicator: " + label);
        }
        return sibling.getElementsByClass("txt").html();
    }
}
