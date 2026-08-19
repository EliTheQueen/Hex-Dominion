package model.trade;

import model.Constants;

public interface TradePolicy {

    boolean allowsToTrade(Constants.ResourceType sell, Constants.ResourceType buy);

    int calculateReceiveAmount(int quantitySold);
}
