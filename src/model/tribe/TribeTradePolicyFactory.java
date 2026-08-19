package model.tribe;

import model.Constants;
import model.trade.FixedRateTradePolicy;
import model.trade.RestrictedOutputTradePolicy;
import model.trade.TradePolicy;

import java.util.EnumSet;

public class TribeTradePolicyFactory implements java.io.Serializable {

    public TradePolicy create(TribeType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        switch (type) {
            case FARMER, COASTAL:
                return new RestrictedOutputTradePolicy(
                        0.75,
                        EnumSet.of(Constants.ResourceType.FOOD)
                );

            case MOUNTAIN:
                return new RestrictedOutputTradePolicy(
                        0.75,
                        EnumSet.of(
                                Constants.ResourceType.STONE,
                                Constants.ResourceType.IRON
                        )
                );

            case MERCHANT:
                return new FixedRateTradePolicy(0.80);

            case WARRIOR:
                throw new IllegalStateException("Warrior tribe does not provide resource trade");

            default:
                throw new IllegalArgumentException("Unsupported tribe type: " + type);
        }
    }
}
