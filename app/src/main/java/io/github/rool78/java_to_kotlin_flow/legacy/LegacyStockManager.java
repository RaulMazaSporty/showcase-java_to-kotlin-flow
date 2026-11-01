package io.github.rool78.java_to_kotlin_flow.legacy;

public interface LegacyStockManager {
    void subscribe(String symbol, StockPriceListener listener);
    void unsubscribe(String symbol, StockPriceListener listener);
}
