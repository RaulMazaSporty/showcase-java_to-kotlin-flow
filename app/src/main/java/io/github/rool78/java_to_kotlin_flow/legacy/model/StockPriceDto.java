package io.github.rool78.java_to_kotlin_flow.legacy.model;

public final class StockPriceDto {
    public final String symbol;
    public final long priceInCents;
    public final long previousCloseInCents;
    public final long timestampMillis;

    public StockPriceDto(String symbol, long priceInCents, long previousCloseInCents,
                       long timestampMillis) {
        this.symbol = symbol;
        this.priceInCents = priceInCents;
        this.previousCloseInCents = previousCloseInCents;
        this.timestampMillis = timestampMillis;
    }
}
