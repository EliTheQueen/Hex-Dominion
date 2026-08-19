package model.tribe.mission;

import model.tribe.Tribe;

public interface TribeMissionRewardApplier extends java.io.Serializable {
    boolean canApply(Tribe tribe, TribeMissionReward reward);
    void apply(Tribe tribe, TribeMissionReward reward);
}
