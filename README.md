# Stock Watch: Java callbacks to Kotlin Flow

A small Android application showing how to adapt an existing Java callback API to
Kotlin Flow without rewriting the underlying connection mechanism.

Created as a final project for the **kt.academy Kotlin Coroutines Mastery course**.
All prices are fictional. A Java simulator runs locally for real time updates

## Overview

```
    Screen[StocksScreen · Compose]
    VM[StocksViewModel · StateFlow]
    VM --> UseCase[ObserveWatchlistUseCase · latest price per symbol]
    UseCase --> Repo[StocksRepository]
    Repo --> DataSource[StockPricesDataSource · callbackFlow]
    DataSource --> Java[LegacyStockManager · Java listeners]
```


1. `legacy/LegacyStockManager.java`: the contract the adapter must respect.
2. `data/StockPricesRealTimeDataSource.kt`: the bridge and resource cleanup.

## Demo controls

The bottom panel selects the observed companies: Apple, Microsoft, Alphabet,
NVIDIA and Amazon. The first three are selected initially. A selection change
uses `flatMapLatest` to cancel the previous observation (removing its listeners)
and register the new symbol set

**Interrupt feed** Forces error. The failure travels through `callbackFlow`, cleans up listeners,
and shows the normal interrupted-feed UI. 

**Reconnect** registers the currently
selected symbols again. Interruption is a one-shot simulated error; the Java
scheduler remains alive so new subscriptions can recover.
