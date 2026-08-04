package model.tribe;

import model.Constants;
import model.Player;
import model.ResourceAmount;

public class TribeGiftService {

    public boolean sendGift(
            Player player,
            Tribe tribe,
            Constants.ResourceType resourceType,
            int amount
    ) {
        if (player == null
                || tribe == null
                || resourceType == null) {
            throw new IllegalArgumentException(
                    "arguments must not be null"
            );
        }

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "amount must be greater than zero"
            );
        }

        if (!tribe.isDiscovered()) {
            return false;
        }

        if (tribe.isDefeated()) {
            return false;
        }

        if (tribe.getRelation().isEnemy()) {
            return false;
        }

        ResourceAmount gift =
                toResourceAmount(
                        resourceType,
                        amount
                );

        if (!player.spend(gift)) {
            return false;
        }

        int relationGain =
                calculateRelationGain(
                        resourceType,
                        amount
                );

        if (relationGain > 0) {
            tribe.getRelation().increase(
                    relationGain
            );
        }

        return true;
    }

    private int calculateRelationGain(
            Constants.ResourceType resourceType,
            int amount
    ) {
        switch (resourceType) {
            case FOOD:
            case WOOD:
                return (amount / 10) * 2;

            case STONE:
                return (amount / 10) * 3;

            case IRON:
                return (amount / 5) * 3;

            default:
                throw new IllegalArgumentException(
                        "Unsupported resource type: "
                                + resourceType
                );
        }
    }

    private ResourceAmount toResourceAmount(
            Constants.ResourceType resourceType,
            int amount
    ) {
        switch (resourceType) {
            case FOOD:
                return ResourceAmount.of(
                        amount,
                        0,
                        0,
                        0
                );

            case WOOD:
                return ResourceAmount.of(
                        0,
                        amount,
                        0,
                        0
                );

            case STONE:
                return ResourceAmount.of(
                        0,
                        0,
                        amount,
                        0
                );

            case IRON:
                return ResourceAmount.of(
                        0,
                        0,
                        0,
                        amount
                );

            default:
                throw new IllegalArgumentException(
                        "Unsupported resource type: "
                                + resourceType
                );
        }
    }
}