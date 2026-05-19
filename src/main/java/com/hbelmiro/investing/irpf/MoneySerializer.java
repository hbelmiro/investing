package com.hbelmiro.investing.irpf;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.javamoney.moneta.Money;

import java.io.IOException;
import java.math.BigDecimal;

public class MoneySerializer extends JsonSerializer<Money> {

    @Override
    public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeNumber(value.getNumber().numberValue(BigDecimal.class));
    }
}
