package model.tribe.behavior;

import model.tribe.Tribe;
import model.tribe.TribeRelationStatus;
import model.tribe.TribeType;

public class DefaultTribeBehaviorStrategy implements TribeBehaviorStrategy {

    @Override
    public TribeTurnAction chooseAction(Tribe tribe, TribeTurnContext context) {
        if (tribe == null || context == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        if (tribe.isDefeated()) {
            return TribeTurnAction.NONE;
        }

        if (context.isCampUnderAttack()) {
            return TribeTurnAction.DEFEND_CAMP;
        }

        TribeRelationStatus status = tribe.getRelation().getStatus();

        if (status == TribeRelationStatus.ENEMY) {
            int guardCap = tribe.getType() == TribeType.WARRIOR ? 5 : 3;

            if (context.getCurrentGuardCount() < guardCap && context.getCurrentTurn() % 3 == 0) {
                return TribeTurnAction.PRODUCE_GUARD;
            }

            // Proactive raids are optional; mandatory enemy behavior is camp defense
            // plus capped guard production, so no disconnected direct-damage action remains.
            return TribeTurnAction.NONE;
        }

        if (status == TribeRelationStatus.FRIENDLY || status == TribeRelationStatus.ALLIED) {

            if (!context.isActiveMissionExists() && context.getCurrentTurn() % 5 == 0) {
                return TribeTurnAction.OFFER_MISSION;
            }

            return TribeTurnAction.OFFER_TRADE;
        }

        return TribeTurnAction.NONE;
    }
}
