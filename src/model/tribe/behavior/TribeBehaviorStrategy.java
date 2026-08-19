package model.tribe.behavior;

import model.tribe.Tribe;

public interface TribeBehaviorStrategy extends java.io.Serializable {

    TribeTurnAction chooseAction(Tribe tribe, TribeTurnContext context);
}
