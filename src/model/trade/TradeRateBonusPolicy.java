package model.trade;

import model.Constants;

/** Applies a permanent percentage improvement to another trade policy's output. */
public final class TradeRateBonusPolicy implements TradePolicy, java.io.Serializable {
    private final TradePolicy basePolicy;
    private final int bonusPercent;

    public TradeRateBonusPolicy(TradePolicy basePolicy, int bonusPercent) {
        if (basePolicy == null || bonusPercent < 0) throw new IllegalArgumentException("invalid trade bonus");
        this.basePolicy = basePolicy;
        this.bonusPercent = bonusPercent;
    }

    @Override public boolean allowsToTrade(Constants.ResourceType sell, Constants.ResourceType buy) {
        return basePolicy.allowsToTrade(sell, buy);
    }

    @Override public int calculateReceiveAmount(int quantitySold) {
        int base = basePolicy.calculateReceiveAmount(quantitySold);
        return (int) Math.floor(base * (1.0 + bonusPercent / 100.0));
    }
}
