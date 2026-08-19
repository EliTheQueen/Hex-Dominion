import model.Constants;
import model.GameState;
import model.HexCoordinate;
import model.ResourceAmount;
import model.combat.CombatReport;
import model.military.Swordsman;
import model.tribe.DiplomacyResult;
import model.tribe.Tribe;
import model.tribe.TribeAllianceBenefit;
import model.tribe.TribeType;
import model.tribe.mission.MissionActionResult;
import model.tribe.mission.TribeMission;

public final class TribeDiplomacyLifecycleTest {
    public static void main(String[] args) {
        giftIsAtomic();
        peaceWaitsForThreeConsecutiveNonAttackTurns();
        recentMissionFailureBlocksAllianceForFiveTurns();
        allianceRestrictionsAndAutomaticRemovalWork();
        everyAllianceBenefitChangesGameplayAndIsReversible();
        System.out.println("TribeDiplomacyLifecycleTest passed");
    }

    private static void giftIsAtomic() {
        GameState state = new GameState(15, 13, 140L);
        Tribe farmer = find(state, TribeType.FARMER);
        farmer.discover();
        int foodBefore = state.getPlayer().getResources().get(Constants.ResourceType.FOOD);
        require(!state.giftTribe(farmer, Constants.ResourceType.FOOD, 1),
                "a gift with zero relation gain is rejected");
        require(state.getPlayer().getResources().get(Constants.ResourceType.FOOD) == foodBefore,
                "rejected gift spends no resource");
        require(state.giftTribe(farmer, Constants.ResourceType.FOOD, 10), "valid gift succeeds");
        require(state.getPlayer().getResources().get(Constants.ResourceType.FOOD) == foodBefore - 10,
                "valid gift spends exactly once");
        require(farmer.getRelation().getScore() == 2, "valid gift applies exact relation gain");
    }

    private static void peaceWaitsForThreeConsecutiveNonAttackTurns() {
        GameState state = new GameState(15, 13, 141L);
        Tribe warrior = find(state, TribeType.WARRIOR);
        warrior.discover();
        warrior.getRelation().becomeEnemy();
        state.getPlayer().addResources(ResourceAmount.of(30, 30, 0, 30));
        int foodBefore = state.getPlayer().getResources().get(Constants.ResourceType.FOOD);

        require(state.requestPeace(warrior) == DiplomacyResult.SUCCESS, "peace request is paid");
        require(state.isPeacePending(warrior), "peace enters pending state");
        require(warrior.getRelation().isEnemy(), "tribe remains an enemy while peace is pending");
        require(state.getPlayer().getResources().get(Constants.ResourceType.FOOD) == foodBefore - 30,
                "peace cost is spent exactly once");
        require(state.requestPeace(warrior) == DiplomacyResult.PEACE_ALREADY_PENDING,
                "pending peace cannot be paid twice");

        state.endTurn();
        require(state.getPeaceProgress(warrior) == 1, "one peaceful turn is recorded");
        Swordsman attacker = new Swordsman(validNeighbour(state, warrior.getCampCoordinate()));
        state.getPlayer().addUnit(attacker);
        CombatReport report = state.attackTribeCamp(attacker, warrior);
        require(report != null, "camp attack is routed through combat");
        require(state.getPeaceProgress(warrior) == 0, "attack resets peace progress");

        state.endTurn();
        state.endTurn();
        require(warrior.getRelation().isEnemy(), "enemy state remains through two peaceful turns");
        state.endTurn();
        require(!state.isPeacePending(warrior), "peace completes after three peaceful turns");
        require(warrior.getRelation().getScore() == -10, "completed peace sets relation to -10");
    }

    private static void recentMissionFailureBlocksAllianceForFiveTurns() {
        GameState state = new GameState(15, 13, 142L);
        Tribe mountain = find(state, TribeType.MOUNTAIN);
        mountain.discover();
        mountain.getRelation().setScore(20);
        require(state.requestMission(mountain) == MissionActionResult.SUCCESS, "mission accepts");
        TribeMission mission = state.getMission(mountain);
        for (int i = 0; i < mission.getTotalTurns(); i++) state.endTurn();
        require(state.getMissionFailureTurn(mountain) == state.getCurrentTurn(),
                "failure turn is retained");
        mountain.getRelation().setScore(70);
        require(state.requestAlliance(mountain) == DiplomacyResult.RECENT_MISSION_FAILURE,
                "recent failure blocks alliance");
        for (int i = 0; i < 4; i++) state.endTurn();
        mountain.getRelation().setScore(70);
        require(state.requestAlliance(mountain) == DiplomacyResult.RECENT_MISSION_FAILURE,
                "alliance remains blocked inside five-turn window");
        state.endTurn();
        mountain.getRelation().setScore(70);
        require(state.requestAlliance(mountain) == DiplomacyResult.SUCCESS,
                "alliance unlocks after five turns");
    }

    private static void allianceRestrictionsAndAutomaticRemovalWork() {
        GameState state = new GameState(15, 13, 143L);
        Tribe farmer = find(state, TribeType.FARMER);
        Tribe mountain = find(state, TribeType.MOUNTAIN);
        farmer.discover(); mountain.discover();
        farmer.getRelation().setScore(70); mountain.getRelation().setScore(70);
        require(state.requestAlliance(farmer) == DiplomacyResult.SUCCESS, "Farmer alliance forms");
        require(state.requestAlliance(mountain) == DiplomacyResult.ALLIANCE_RESTRICTED,
                "Farmer and Mountain alliances conflict");
        farmer.getRelation().setScore(69);
        require(!state.isAllied(farmer), "relation below 70 removes alliance automatically");
        require(!state.isAllianceBenefitActive(farmer), "removed alliance disables its benefit");

        GameState warriorState = new GameState(15, 13, 144L);
        Tribe warrior = find(warriorState, TribeType.WARRIOR);
        Tribe merchant = find(warriorState, TribeType.MERCHANT);
        warrior.discover(); merchant.discover();
        warrior.getRelation().setScore(70); merchant.getRelation().setScore(70);
        require(warriorState.requestAlliance(warrior) == DiplomacyResult.SUCCESS, "Warrior alliance forms");
        require(warriorState.requestAlliance(merchant) == DiplomacyResult.ALLIANCE_RESTRICTED,
                "Warrior alliance is exclusive");
    }

    private static void everyAllianceBenefitChangesGameplayAndIsReversible() {
        assertBenefit(TribeType.FARMER, TribeAllianceBenefit.FARMER, 2, 0, 0, 0);
        assertBenefit(TribeType.WARRIOR, TribeAllianceBenefit.WARRIOR, 0, 0, 0, 1);
        assertBenefit(TribeType.MERCHANT, TribeAllianceBenefit.MERCHANT, 0, 1, 1, 0);
        assertBenefit(TribeType.MOUNTAIN, TribeAllianceBenefit.MOUNTAIN, 0, 0, 2, 0);
        assertBenefit(TribeType.COASTAL, TribeAllianceBenefit.COASTAL, 1, 1, 0, 0);

        GameState liveState = new GameState(15, 13, 145L);
        Tribe warrior = find(liveState, TribeType.WARRIOR);
        warrior.discover(); warrior.getRelation().setScore(70);
        require(liveState.requestAlliance(warrior) == DiplomacyResult.SUCCESS,
                "live Warrior alliance forms");
        liveState.endTurn();
        require(liveState.getPlayer().getResources().get(Constants.ResourceType.IRON) == 1,
                "active alliance income is applied by the turn engine");
        warrior.getRelation().setScore(69);
        liveState.endTurn();
        require(liveState.getPlayer().getResources().get(Constants.ResourceType.IRON) == 1,
                "broken alliance stops recurring income immediately");
    }

    private static void assertBenefit(TribeType type, TribeAllianceBenefit expected,
                                      int food, int wood, int stone, int iron) {
        GameState state = new GameState(15, 13, 146L);
        Tribe tribe = find(state, type);
        tribe.discover(); tribe.getRelation().setScore(70);
        require(state.requestAlliance(tribe) == DiplomacyResult.SUCCESS, type + " alliance forms");
        require(state.getAllianceBenefit(tribe) == expected, type + " benefit is exposed");
        require(state.getAllianceBenefitDescription(tribe).startsWith("ACTIVE"),
                type + " active state is exposed to UI");
        assertAmount(state.getActiveAllianceTurnIncome(), food, wood, stone, iron,
                type + " recurring income");

        tribe.getRelation().setScore(69);
        assertAmount(state.getActiveAllianceTurnIncome(), 0, 0, 0, 0,
                type + " income is removed when alliance breaks");
        require(state.getAllianceBenefitDescription(tribe).startsWith("Inactive"),
                type + " inactive state is exposed to UI");
    }

    private static HexCoordinate validNeighbour(GameState state, HexCoordinate center) {
        for (HexCoordinate coordinate : center.findNeighbours()) {
            if (state.getMap().containsCoordinate(coordinate)) return coordinate;
        }
        throw new AssertionError("no valid neighbour for " + center);
    }

    private static Tribe find(GameState state, TribeType type) {
        for (Tribe tribe : state.getTribes()) if (tribe.getType() == type) return tribe;
        throw new AssertionError("missing " + type);
    }

    private static void assertAmount(ResourceAmount amount, int food, int wood, int stone, int iron,
                                     String message) {
        require(amount.get(Constants.ResourceType.FOOD) == food
                        && amount.get(Constants.ResourceType.WOOD) == wood
                        && amount.get(Constants.ResourceType.STONE) == stone
                        && amount.get(Constants.ResourceType.IRON) == iron,
                message + " has incorrect values");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
