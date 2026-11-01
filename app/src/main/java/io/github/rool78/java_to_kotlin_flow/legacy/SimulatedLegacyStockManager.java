package io.github.rool78.java_to_kotlin_flow.legacy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.rool78.java_to_kotlin_flow.legacy.model.StockPriceDto;

/**
 * Simulates some Legacy Java socket client devs don't look forward to deal with
 * */
public final class SimulatedLegacyStockManager implements LegacyStockManager, AutoCloseable {
    private static final long FIRST_PRICE_DELAY_MILLIS = 2_000;
    private final Map<String, Set<StockPriceListener>> listeners = new LinkedHashMap<>();
    private final Map<StockPriceListener, Long> readyAtNanos = new LinkedHashMap<>();
    private final Map<String, Long> previousCloses = new LinkedHashMap<>();
    private final ScheduledExecutorService connection = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "legacy-stock-feed");
        thread.setDaemon(true);
        return thread;
    });
    private long tick;
    private boolean closed;

    public SimulatedLegacyStockManager() {
        // Fictional prices: this is a coroutine demonstration, not market data.
        previousCloses.put("AAPL", 21500L);
        previousCloses.put("MSFT", 42000L);
        previousCloses.put("GOOGL", 17500L);
        previousCloses.put("NVDA", 14000L);
        previousCloses.put("AMZN", 19000L);
        // Fixed delay avoids catch-up bursts when Android resumes a cached process.
        connection.scheduleWithFixedDelay(this::publish, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void subscribe(String symbol, StockPriceListener listener) {
        if (closed) throw new IllegalStateException("Feed is closed");
        if (!previousCloses.containsKey(symbol)) throw new IllegalArgumentException("Unknown symbol");
        listeners.computeIfAbsent(symbol, ignored -> new LinkedHashSet<>()).add(listener);
        readyAtNanos.putIfAbsent(listener, System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(FIRST_PRICE_DELAY_MILLIS));
    }

    @Override
    public synchronized void unsubscribe(String symbol, StockPriceListener listener) {
        Set<StockPriceListener> registered = listeners.get(symbol);
        if (registered == null) return;
        registered.remove(listener);
        if (registered.isEmpty()) listeners.remove(symbol);
        if (listeners.values().stream().noneMatch(group -> group.contains(listener))) {
            readyAtNanos.remove(listener);
        }
    }

    public void interruptFeed() {
        Set<StockPriceListener> affected = new LinkedHashSet<>();
        synchronized (this) {
            for (Set<StockPriceListener> registered : listeners.values()) {
                affected.addAll(registered);
            }
        }
        for (StockPriceListener listener : affected) {
            listener.onError(new IllegalStateException("Simulated feed interruption"));
        }
    }

    /** A fast finite burst on the Java feed thread, using only normal price callbacks. */
    public synchronized void burstUpdates() {
        if (closed) return;
        connection.execute(() -> {
            for (int i = 0; i < 256; i++) {
                // Deliberate stress injection bypasses the normal first-price delay.
                publish(true);
            }
        });
    }

    private void publish() {
        publish(false);
    }

    private void publish(boolean burst) {
        List<Runnable> callbacks = new ArrayList<>();
        synchronized (this) {
            if (closed) return;
            tick++;
            int index = 0;
            for (Map.Entry<String, Long> entry : previousCloses.entrySet()) {
                long change = Math.round(Math.sin(tick * 0.35 + index++) * 180);
                StockPriceDto price = new StockPriceDto(entry.getKey(), entry.getValue() + change,
                        entry.getValue(), System.currentTimeMillis());
                Set<StockPriceListener> registered = listeners.get(entry.getKey());
                if (registered != null) {
                    for (StockPriceListener listener : registered) {
                        if (!burst && System.nanoTime() < readyAtNanos.get(listener)) continue;
                        callbacks.add(() -> listener.onPrice(price));
                    }
                }
            }
        }
        // External callbacks run outside the manager lock.
        for (Runnable callback : callbacks) callback.run();
    }

    @Override
    public synchronized void close() {
        closed = true;
        listeners.clear();
        readyAtNanos.clear();
        connection.shutdownNow();
    }
}
