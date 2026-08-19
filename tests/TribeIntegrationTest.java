import model.GameState;
import model.tribe.*;
import model.tribe.mission.*;

public final class TribeIntegrationTest {
    public static void main(String[] args) {
        GameState state = new GameState(15, 13);
        require(state.getTribes().size() == 5, "five tribes generated");
        Tribe farmer = find(state, TribeType.FARMER);
        Tribe mountain = find(state, TribeType.MOUNTAIN);
        farmer.discover(); mountain.discover();
        farmer.getRelation().setScore(20);
        require(state.requestMission(farmer) == MissionActionResult.SUCCESS, "mission accepted");
        TribeMission mission = state.getMission(farmer);
        mission.refreshCompletionState();
        require(mission.getStatus() == TribeMissionStatus.READY_TO_TURN_IN, "ready state");
        require(state.turnInMission(farmer) == MissionActionResult.SUCCESS, "mission turn in");
        require(state.requestMission(farmer) == MissionActionResult.COOLDOWN, "mission cooldown");

        farmer.getRelation().setScore(70); mountain.getRelation().setScore(70);
        require(state.requestAlliance(farmer) == DiplomacyResult.SUCCESS, "farmer alliance");
        require(state.requestAlliance(mountain) == DiplomacyResult.ALLIANCE_RESTRICTED,
                "farmer/mountain restriction");
        int happiness = state.getHappinessScore();
        require(state.declareWar(farmer) == DiplomacyResult.SUCCESS, "declare war");
        require(state.getHappinessScore() == happiness - 15, "allied attack happiness");

        GameState hostileState = new GameState(15, 13);
        Tribe hostile = find(hostileState, TribeType.WARRIOR); hostile.discover(); hostile.getRelation().becomeEnemy();
        model.HexCoordinate near = hostile.getCampCoordinate().findNeighbours().get(0);
        model.military.Swordsman victim = new model.military.Swordsman(near); hostileState.getPlayer().addUnit(victim);
        hostileState.endTurn();
        require(!victim.isAlive() || !hostileState.getPlayer().getUnits().contains(victim),
                "enemy tribe performs prioritized hostile action");
        System.out.println("TribeIntegrationTest passed");
    }

    private static Tribe find(GameState state, TribeType type) {
        for (Tribe tribe : state.getTribes()) if (tribe.getType() == type) return tribe;
        throw new AssertionError("missing " + type);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
