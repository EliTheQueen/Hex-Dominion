package model.tribe.behavior;

import model.tribe.Tribe;

public interface TribeBehaviorStrategy {

    TribeTurnAction chooseAction(Tribe tribe, TribeTurnContext context);
}