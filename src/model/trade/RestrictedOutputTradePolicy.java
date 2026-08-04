package model.trade;

import model.Constants;

import java.util.Objects;
import java.util.Set;

//Trade با نرخ ثابت، ولی فقط برای مجموعه مشخصی از منابع دریافتی.
public class RestrictedOutputTradePolicy implements TradePolicy {
    private final double rate;
    private final Set<Constants.ResourceType> buyResources;

    public RestrictedOutputTradePolicy(double rate, Set<Constants.ResourceType> buyResources) {
        if (rate <= 0 || rate >= 1)
            throw new IllegalArgumentException("rate must be positive and less than 1");
        if (buyResources == null || buyResources.isEmpty())
            throw new IllegalArgumentException("buyResources must not be null or empty");
        if (buyResources.contains(null))
            throw new IllegalArgumentException("buyResources must not contains null");

        this.rate = rate;
        this.buyResources = buyResources;
    }

    @Override
    public boolean allowsToTrade(Constants.ResourceType sell, Constants.ResourceType buy) {
        Objects.requireNonNull(sell, "sell must not be null");
        Objects.requireNonNull(buy, "buy must not be null");

        return buyResources.contains(buy) && buy != sell;
    }

    @Override
    public int calculateReceiveAmount(int quantitySold) {
        if (quantitySold <= 0)
            throw new IllegalArgumentException("quantitySold must be positive");

        return (int) Math.floor(quantitySold * rate);
    }
}
