package model.tribe.mission;

import model.Player;
import model.ResourceAmount;
import model.tribe.Tribe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//این Service کل Missionها را مدیریت می‌کند.
public class TribeMissionService implements java.io.Serializable {

    private static final int REQUIRED_RELATION = 20;
    private static final int FAILURE_RELATION_PENALTY = 10;
    private static final int CANCEL_RELATION_PENALTY = 5;

    private final Player player;
    private final TribeMissionRewardApplier rewardApplier;

    private final Map<String, TribeMission> activeMissionsByTribeId = new HashMap<>();

    public TribeMissionService(Player player) {
        this(player, new TribeMissionRewardApplier() {
            @Override public boolean canApply(Tribe tribe, TribeMissionReward reward) {
                return reward.getEffects().isEmpty();
            }
            @Override public void apply(Tribe tribe, TribeMissionReward reward) { }
        });
    }

    public TribeMissionService(Player player, TribeMissionRewardApplier rewardApplier) {
        if (player == null || rewardApplier == null) {
            throw new IllegalArgumentException("player and reward applier must not be null");
        }

        this.player = player;
        this.rewardApplier = rewardApplier;
    }

    public MissionActionResult acceptMission(TribeMission mission) {
        if (mission == null) {
            return MissionActionResult.INVALID_REQUEST;
        }

        Tribe tribe = mission.getTribe();

        if (!tribe.isDiscovered()) {
            return MissionActionResult.TRIBE_NOT_DISCOVERED;
        }

        if (tribe.getRelation().isEnemy()) {
            return MissionActionResult.TRIBE_IS_ENEMY;
        }

        if (tribe.getRelation().getScore() < REQUIRED_RELATION) {
            return MissionActionResult.RELATION_TOO_LOW;
        }

        if (activeMissionsByTribeId.containsKey(tribe.getId())) {
            return MissionActionResult.ACTIVE_MISSION_EXISTS;
        }

        mission.accept();

        activeMissionsByTribeId.put(tribe.getId(), mission);

        return MissionActionResult.SUCCESS;
    }

    public MissionActionResult claimMission(TribeMission mission) {
        if (mission == null) {
            return MissionActionResult.INVALID_REQUEST;
        }

        mission.refreshCompletionState();

        if (mission.getStatus() != TribeMissionStatus.READY_TO_TURN_IN) {
            return MissionActionResult.MISSION_NOT_COMPLETED;
        }

        ResourceAmount rewardResources = mission.getReward().getResources();

        if (!player.canStore(rewardResources)) {
            return MissionActionResult.INSUFFICIENT_STORAGE;
        }

        if (!rewardApplier.canApply(mission.getTribe(), mission.getReward())) {
            return MissionActionResult.MISSION_NOT_COMPLETED;
        }

        if (!mission.getObjective().fulfill(player)) return MissionActionResult.MISSION_NOT_COMPLETED;

        player.addResources(rewardResources);

        int relationReward = mission.getReward().getRelationReward();

        if (relationReward > 0) {
            mission.getTribe().getRelation().increase(relationReward);
        }

        rewardApplier.apply(mission.getTribe(), mission.getReward());

        mission.complete();

        activeMissionsByTribeId.remove(mission.getTribe().getId());

        return MissionActionResult.SUCCESS;
    }

    public MissionActionResult cancelMission(TribeMission mission) {
        if (mission == null) {
            return MissionActionResult.INVALID_REQUEST;
        }

        if (mission.getStatus() != TribeMissionStatus.ACTIVE && mission.getStatus() != TribeMissionStatus.READY_TO_TURN_IN) {
            return MissionActionResult.MISSION_NOT_ACTIVE;
        }

        mission.cancel();

        mission.getTribe().getRelation().decrease(CANCEL_RELATION_PENALTY);

        activeMissionsByTribeId.remove(mission.getTribe().getId());

        return MissionActionResult.SUCCESS;
    }

    public java.util.List<Tribe> advanceOneTurn() {
        java.util.List<Tribe> failures = new java.util.ArrayList<>();
        Map<String, TribeMission> snapshot = new HashMap<>(activeMissionsByTribeId);

        for (TribeMission mission : snapshot.values()) {

            TribeMissionStatus previousStatus = mission.getStatus();

            mission.advanceOneTurn();

            if (previousStatus != TribeMissionStatus.FAILED && mission.getStatus() == TribeMissionStatus.FAILED) {

                mission.getTribe().getRelation().decrease(FAILURE_RELATION_PENALTY);

                activeMissionsByTribeId.remove(mission.getTribe().getId());
                failures.add(mission.getTribe());
            }
        }
        return failures;
    }

    public TribeMission getActiveMission(Tribe tribe) {
        if (tribe == null) {
            return null;
        }

        return activeMissionsByTribeId.get(tribe.getId());
    }

    public Map<String, TribeMission> getActiveMissions() {
        return Collections.unmodifiableMap(new HashMap<>(activeMissionsByTribeId));
    }

    /** Removes mission state after camp defeat without applying another diplomacy penalty. */
    public void invalidateMission(Tribe tribe) {
        if (tribe == null) return;
        TribeMission mission = activeMissionsByTribeId.remove(tribe.getId());
        if (mission != null) mission.cancel();
    }
}
