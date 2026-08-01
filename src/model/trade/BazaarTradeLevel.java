package model.trade;

public enum BazaarTradeLevel {
    LEVEL_ONE(10, 0.5),
    LEVEL_TWO(100, 0.6),
    LEVEL_THREE(500, 0.7),
    ;

    private final double rate;
    private final int sellCount;

    BazaarTradeLevel(int sellCount, double rate) {
        this.rate = rate;
        this.sellCount = sellCount;
    }

    public double getRate() {
        return rate;
    }
    public int getSellCount() {
        return sellCount;
    }
}
