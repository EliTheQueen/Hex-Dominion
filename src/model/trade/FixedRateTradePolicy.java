package model.trade;

import model.Constants;

import java.util.Objects;

public class FixedRateTradePolicy implements TradePolicy {

    private final double rate;

    public FixedRateTradePolicy(double rate) {
        if (rate < 0 || rate > 1)
            throw new IllegalArgumentException("Invalid rate " + rate);

        this.rate = rate;
    }

    @Override
    public boolean allowsToTrade(Constants.ResourceType sell, Constants.ResourceType buy) {
        Objects.requireNonNull(sell, "sell resource type must not be null");
        Objects.requireNonNull(buy, "buy resource type must not be null");

        return sell != buy;
    }

    @Override
    public int calculateReceiveAmount(int quantitySold) {
        if (quantitySold <= 0)
            throw new IllegalArgumentException("quantity sold must be greater than 0");

        return (int) Math.floor(quantitySold * rate);
    }

    public double getRate() {
        return rate;
    }
}
