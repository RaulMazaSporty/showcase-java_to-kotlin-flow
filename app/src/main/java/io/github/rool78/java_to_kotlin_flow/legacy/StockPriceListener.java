package io.github.rool78.java_to_kotlin_flow.legacy;

import io.github.rool78.java_to_kotlin_flow.legacy.model.StockPriceDto;

public interface StockPriceListener {
    void onPrice(StockPriceDto price);
    void onError(Throwable cause);
}
