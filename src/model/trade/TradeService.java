package model.trade;

import model.Constants;
import model.Player;
import model.ResourceAmount;

import java.util.Objects;

public final class TradeService {

    public boolean complete(
            Player player,
            TradePolicy policy,
            Constants.ResourceType sell,
            Constants.ResourceType buy,
            int quantitySold
    ) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(sell, "sell must not be null");
        Objects.requireNonNull(buy, "buy must not be null");
        if (quantitySold <= 0) {
            throw new IllegalArgumentException("quantitySold must be greater than zero");
        }

        if (!policy.allowsToTrade(sell, buy)) {
            return false;
        }

        int receivedQuantity = policy.calculateReceiveAmount(quantitySold);

        if (receivedQuantity <= 0) {
            return false;
        }

        ResourceAmount sellAmount = createResourceAmount(sell, quantitySold);
        ResourceAmount receiveAmount = createResourceAmount(buy, receivedQuantity);
        if (!player.canAfford(sellAmount)) {
            return false;
        }

        if (!player.canStore(receiveAmount)) {
            return false;
        }

        boolean spentSuccessfully = player.spend(sellAmount);

        if (!spentSuccessfully) {
            return false;
        }

        player.addResources(receiveAmount);
        return true;
    }

    private ResourceAmount createResourceAmount(
            Constants.ResourceType resourceType,
            int amount
    ) {
        switch (resourceType) {
            case FOOD:
                return ResourceAmount.of(amount, 0, 0, 0);

            case WOOD:
                return ResourceAmount.of(0, amount, 0, 0);

            case STONE:
                return ResourceAmount.of(0, 0, amount, 0);

            case IRON:
                return ResourceAmount.of(0, 0, 0, amount);

            default:
                throw new IllegalArgumentException(
                        "Unsupported resource type: " + resourceType
                );
        }
    }
}
