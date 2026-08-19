package model.tribe.behavior;

import model.tribe.Tribe;

public class TribeTurnService implements java.io.Serializable {

    private final TribeBehaviorStrategy behaviorStrategy;

    public TribeTurnService(TribeBehaviorStrategy behaviorStrategy) {
        if (behaviorStrategy == null) {
            throw new IllegalArgumentException("behaviorStrategy must not be null");
        }

        this.behaviorStrategy = behaviorStrategy;
    }

    public TribeTurnAction processTurn(Tribe tribe, TribeTurnContext context) {
        if (tribe == null || context == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        if (!tribe.isDiscovered() || tribe.isDefeated()) {
            return TribeTurnAction.NONE;
        }

        return behaviorStrategy.chooseAction(tribe, context);
    }
}
