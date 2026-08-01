package model.trade;

import model.Constants;

import java.util.Objects;

public class BazaarTradePolicy implements TradePolicy {

    private final BazaarTradeLevel level;

    public BazaarTradePolicy(BazaarTradeLevel level) {
        Objects.requireNonNull(level, "BazaarTradeLevel cannot be null");

        this.level = level;
    }

    @Override
    public boolean allowsToTrade(Constants.ResourceType sell, Constants.ResourceType buy) {
        Objects.requireNonNull(sell, "sell cannot be null");
        Objects.requireNonNull(buy, "buy cannot be null");

        return sell!=buy;

    }

    @Override
    public int calculateReceiveAmount(int quantitySold) {
        if (quantitySold <= 0) {
            throw new IllegalArgumentException("quantitySold must be greater than 0");
        }

        if (quantitySold != getSellCount()) {
            throw new IllegalArgumentException("quantitySold must be equal to " + getSellCount());
        }

        return (int) Math.floor(quantitySold * getRate());
    }

    public double getRate() {
        return level.getRate();
    }
    public int getSellCount() {
        return level.getSellCount();
    }
}
