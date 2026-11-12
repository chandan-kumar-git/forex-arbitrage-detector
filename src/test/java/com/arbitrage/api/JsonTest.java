package com.arbitrage.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    void stringsAreQuotedAndEscaped() {
        assertEquals("\"say \\\"hi\\\"\\n\"", Json.string("say \"hi\"\n"));
        assertEquals("\"INR → EUR\"", Json.string("INR → EUR"));
    }

    @Test
    void numbersUseADotAndNeverNaN() {
        assertEquals("1.08", Json.number(1.08));
        assertEquals("0", Json.number(Double.NaN));
        assertEquals("0", Json.number(Double.POSITIVE_INFINITY));
    }

    @Test
    void listsAndMatrices() {
        assertEquals("[\"USD\",\"EUR\"]", Json.strings(List.of("USD", "EUR")));
        assertEquals("[[1.0,0.5],[2.0,1.0]]", Json.matrix(new double[][]{{1, 0.5}, {2, 1}}));
    }
}
