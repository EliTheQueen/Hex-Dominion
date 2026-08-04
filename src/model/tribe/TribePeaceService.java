package model.tribe;

import model.Player;
import model.ResourceAmount;

public class TribePeaceService {

    private static final ResourceAmount PEACE_COST = ResourceAmount.of(30, 30, 0, 30);

    public DiplomacyResult requestPeace(Player player, Tribe tribe) {
        if (player == null || tribe == null) {
            return DiplomacyResult.INVALID_REQUEST;
        }

        if (!tribe.isDiscovered()) {
            return DiplomacyResult.TRIBE_NOT_DISCOVERED;
        }

        if (tribe.isDefeated()) {
            return DiplomacyResult.TRIBE_DEFEATED;
        }

        if (!tribe.getRelation().isEnemy()) {
            return DiplomacyResult.NOT_ENEMY;
        }

        if (!player.canAfford(PEACE_COST)) {
            return DiplomacyResult.INSUFFICIENT_RESOURCES;
        }

        if (!player.spend(PEACE_COST)) {
            return DiplomacyResult.INSUFFICIENT_RESOURCES;
        }

        tribe.getRelation().setScore(-10);

        return DiplomacyResult.SUCCESS;
    }

    public ResourceAmount getPeaceCost() {
        return PEACE_COST.copy();
    }
}