package model.trade;

import model.Constants;

import java.util.Objects;

public class TradingPostPolicy implements TradePolicy {

    private static final double RATE = 0.80;

    @Override
    public boolean allowsToTrade(Constants.ResourceType sell, Constants.ResourceType buy) {
        Objects.requireNonNull(sell, "sell must not be null");
        Objects.requireNonNull(buy, "buy must not be null");

        return sell != buy;
    }

    @Override
    public int calculateReceiveAmount(int quantitySold) {
        if (quantitySold <= 0)
            throw new IllegalArgumentException("quantitySold must be positive");

        return  (int) Math.floor(quantitySold * RATE);
    }
}
