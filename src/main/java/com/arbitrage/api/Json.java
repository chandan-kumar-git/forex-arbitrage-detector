package com.arbitrage.api;

import java.util.List;

/**
 * Builds small JSON texts by hand, so the project needs no JSON library.
 * Only what the web API sends is supported: strings, numbers, booleans and lists.
 */
public final class Json {

    private Json() {
    }

    /** A JSON string with quotes, backslashes and control characters escaped. */
    public static String string(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** A JSON number. NaN and infinity are not valid JSON, so they become 0. */
    public static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return Double.toString(value);            // always a dot, e.g. 1.08 or 1.0E-12: both valid JSON
    }

    public static String strings(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(string(values.get(i)));
        }
        return out.append(']').toString();
    }

    /** A matrix written row by row: [[1,0.9],[1.1,1]]. */
    public static String matrix(double[][] rows) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append('[');
            for (int j = 0; j < rows[i].length; j++) {
                if (j > 0) {
                    out.append(',');
                }
                out.append(number(rows[i][j]));
            }
            out.append(']');
        }
        return out.append(']').toString();
    }
}
