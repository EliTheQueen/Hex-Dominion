package model.tribe;

import model.Player;
import model.ResourceAmount;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TribePeaceService implements java.io.Serializable {

    private static final ResourceAmount PEACE_COST = ResourceAmount.of(30, 30, 0, 30);
    private static final int REQUIRED_PEACEFUL_TURNS = 3;
    private final Map<String, Integer> peacefulTurnsByTribeId = new HashMap<>();

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

        if (isPeacePending(tribe)) {
            return DiplomacyResult.PEACE_ALREADY_PENDING;
        }

        if (!player.canAfford(PEACE_COST)) {
            return DiplomacyResult.INSUFFICIENT_RESOURCES;
        }

        if (!player.spend(PEACE_COST)) {
            return DiplomacyResult.INSUFFICIENT_RESOURCES;
        }

        peacefulTurnsByTribeId.put(tribe.getId(), 0);

        return DiplomacyResult.SUCCESS;
    }

    /** Advances paid peace requests and returns the tribes whose peace completed. */
    public List<Tribe> advanceOneTurn(Collection<Tribe> tribes) {
        List<Tribe> completed = new ArrayList<>();
        if (tribes == null) return completed;

        for (Tribe tribe : tribes) {
            if (!isPeacePending(tribe)) continue;
            if (tribe.isDefeated() || !tribe.getRelation().isEnemy()) {
                peacefulTurnsByTribeId.remove(tribe.getId());
                continue;
            }

            int peacefulTurns = peacefulTurnsByTribeId.get(tribe.getId()) + 1;
            if (peacefulTurns >= REQUIRED_PEACEFUL_TURNS) {
                peacefulTurnsByTribeId.remove(tribe.getId());
                tribe.getRelation().setScore(-10);
                completed.add(tribe);
            } else {
                peacefulTurnsByTribeId.put(tribe.getId(), peacefulTurns);
            }
        }
        return completed;
    }

    /** Any successful attack restarts the consecutive non-attack requirement. */
    public void recordAttack(Tribe tribe) {
        if (isPeacePending(tribe)) peacefulTurnsByTribeId.put(tribe.getId(), 0);
    }

    public void cancel(Tribe tribe) {
        if (tribe != null) peacefulTurnsByTribeId.remove(tribe.getId());
    }

    public boolean isPeacePending(Tribe tribe) {
        return tribe != null && peacefulTurnsByTribeId.containsKey(tribe.getId());
    }

    public int getPeacefulTurns(Tribe tribe) {
        return tribe == null ? 0 : peacefulTurnsByTribeId.getOrDefault(tribe.getId(), 0);
    }

    public int getRequiredPeacefulTurns() {
        return REQUIRED_PEACEFUL_TURNS;
    }

    public ResourceAmount getPeaceCost() {
        return PEACE_COST.copy();
    }
}
