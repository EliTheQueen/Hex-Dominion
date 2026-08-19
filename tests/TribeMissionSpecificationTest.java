import model.Building;
import model.Constants;
import model.GameState;
import model.HexCoordinate;
import model.ResourceAmount;
import model.military.MilitaryUnit;
import model.tribe.Tribe;
import model.tribe.TribeType;
import model.tribe.mission.BuildingNearCampObjective;
import model.tribe.mission.KillCountObjective;
import model.tribe.mission.MissionActionResult;
import model.tribe.mission.ResourcePaymentObjective;
import model.tribe.mission.RoadConnectionObjective;
import model.tribe.mission.TribeMission;
import model.tribe.mission.TribeMissionRewardEffect;

public final class TribeMissionSpecificationTest {
    public static void main(String[] args) {
        farmerMissionIsExact();
        merchantMissionIsExactAndGrantsTradeBonus();
        warriorMissionIsExactAndGrantsUnits();
        mountainMissionIsExact();
        coastalMissionIsExactAndDiscountsNextDock();
        failedMissionCooldownIsFiveTurns();
        System.out.println("TribeMissionSpecificationTest passed");
    }

    private static void farmerMissionIsExact() {
        MissionFixture fixture = new MissionFixture(TribeType.FARMER);
        TribeMission mission = fixture.accept();
        require(mission.getTotalTurns() == 5, "Farmer deadline is 5 turns");
        require(mission.getObjective() instanceof ResourcePaymentObjective, "Farmer uses payment objective");
        assertAmount(((ResourcePaymentObjective) mission.getObjective()).getRequiredResources(), 0, 20, 10, 0,
                "Farmer payment");
        assertAmount(mission.getReward().getResources(), 30, 0, 0, 0, "Farmer reward");
        require(mission.getReward().getRelationReward() == 15, "Farmer relation reward is +15");
        mission.refreshCompletionState();
        require(fixture.state.turnInMission(fixture.tribe) == MissionActionResult.SUCCESS,
                "Farmer mission claims");
        require(fixture.state.getPlayer().getResources().get(Constants.ResourceType.FOOD) == 70,
                "Farmer grants 30 Food");
        require(fixture.tribe.getRelation().getScore() == 35, "Farmer grants +15 relation");
    }

    private static void merchantMissionIsExactAndGrantsTradeBonus() {
        MissionFixture fixture = new MissionFixture(TribeType.MERCHANT);
        TribeMission mission = fixture.accept();
        require(mission.getTotalTurns() == 10, "Merchant deadline is 10 turns");
        require(mission.getObjective() instanceof RoadConnectionObjective, "Merchant uses road objective");
        require(mission.getReward().hasEffect(TribeMissionRewardEffect.TRADE_RATE_BONUS_10_PERCENT),
                "Merchant reward declares +10% future trade");
        require(mission.getReward().getRelationReward() == 20, "Merchant relation reward is +20");

        HexCoordinate adjacent = validNeighbour(fixture.state, fixture.tribe.getCampCoordinate(), null);
        HexCoordinate start = null;
        for (HexCoordinate coordinate : adjacent.findNeighbours()) {
            if (fixture.state.getMap().containsCoordinate(coordinate)
                    && coordinate.distanceTo(fixture.tribe.getCampCoordinate()) == 2) {
                start = coordinate;
                break;
            }
        }
        require(start != null, "road test starts two hexes from camp");
        Building ownBuilding = new Building(start, Constants.BuildingType.FARM);
        fixture.state.getPlayer().addBuilding(ownBuilding);
        fixture.state.getMap().getHex(start).setHasBuilding(true);
        fixture.state.getMap().buildRoad(start);
        fixture.state.getMap().buildRoad(adjacent);
        mission.refreshCompletionState();
        require(fixture.state.turnInMission(fixture.tribe) == MissionActionResult.SUCCESS,
                "road from a non-Town-Hall own building completes Merchant mission");
        require(fixture.state.getMissionTradeBonusPercent(fixture.tribe) == 10,
                "Merchant trade bonus becomes permanent for that tribe");
        fixture.state.getPlayer().addResources(ResourceAmount.of(0, 100, 0, 0));
        require(fixture.state.tradeWithTribe(fixture.tribe, Constants.ResourceType.WOOD,
                        Constants.ResourceType.IRON, 50), "Merchant trade succeeds");
        require(fixture.state.getPlayer().getResources().get(Constants.ResourceType.IRON) == 44,
                "Merchant's normal 40 output is improved by 10% to 44");
    }

    private static void warriorMissionIsExactAndGrantsUnits() {
        MissionFixture fixture = new MissionFixture(TribeType.WARRIOR);
        TribeMission mission = fixture.accept();
        require(mission.getTotalTurns() == 8, "Warrior deadline is 8 turns");
        require(mission.getObjective() instanceof KillCountObjective, "Warrior uses kill objective");
        KillCountObjective kills = (KillCountObjective) mission.getObjective();
        require(kills.getRequiredKills() == 2, "Warrior requires exactly 2 kills");
        require(mission.getReward().hasEffect(TribeMissionRewardEffect.THREE_SWORDSMEN),
                "Warrior reward declares 3 Swordsmen");
        require(mission.getReward().getRelationReward() == 20, "Warrior relation reward is +20");
        int militaryBefore = countMilitary(fixture.state);
        HexCoordinate camp = fixture.tribe.getCampCoordinate();
        fixture.state.onEnemyDefeated(new HexCoordinate(camp.getQ() + 6, camp.getR()));
        require(kills.getCurrentKills() == 0, "kill outside radius 5 does not count");
        fixture.state.onEnemyDefeated(new HexCoordinate(camp.getQ() + 5, camp.getR()));
        fixture.state.onEnemyDefeated(new HexCoordinate(camp.getQ() - 5, camp.getR()));
        require(kills.getCurrentKills() == 2, "two kills at radius 5 count");
        require(fixture.state.turnInMission(fixture.tribe) == MissionActionResult.SUCCESS,
                "Warrior mission claims");
        require(countMilitary(fixture.state) == militaryBefore + 3, "Warrior grants exactly 3 Swordsmen");
    }

    private static void mountainMissionIsExact() {
        MissionFixture fixture = new MissionFixture(TribeType.MOUNTAIN);
        TribeMission mission = fixture.accept();
        require(mission.getTotalTurns() == 6, "Mountain deadline is 6 turns");
        require(mission.getObjective() instanceof ResourcePaymentObjective, "Mountain uses payment objective");
        assertAmount(((ResourcePaymentObjective) mission.getObjective()).getRequiredResources(), 0, 15, 0, 10,
                "Mountain payment");
        assertAmount(mission.getReward().getResources(), 0, 0, 20, 0, "Mountain reward");
        require(mission.getReward().getRelationReward() == 15, "Mountain relation reward is +15");
        fixture.state.getPlayer().addResources(ResourceAmount.of(0, 0, 0, 10));
        mission.refreshCompletionState();
        require(fixture.state.turnInMission(fixture.tribe) == MissionActionResult.SUCCESS,
                "Mountain mission claims");
        require(fixture.state.getPlayer().getResources().get(Constants.ResourceType.STONE) == 30,
                "Mountain grants 20 Stone");
    }

    private static void coastalMissionIsExactAndDiscountsNextDock() {
        MissionFixture fixture = new MissionFixture(TribeType.COASTAL);
        TribeMission mission = fixture.accept();
        require(mission.getTotalTurns() == 10, "Coastal deadline is 10 turns");
        require(mission.getObjective() instanceof BuildingNearCampObjective, "Coastal uses building objective");
        BuildingNearCampObjective objective = (BuildingNearCampObjective) mission.getObjective();
        require(objective.getBuildingType() == Constants.BuildingType.DOCK
                        && objective.getMaximumDistance() == 4,
                "Coastal requires a Dock within 4 hexes");
        assertAmount(mission.getReward().getResources(), 30, 0, 0, 0, "Coastal reward");
        require(mission.getReward().hasEffect(TribeMissionRewardEffect.NEXT_DOCK_COST_REDUCTION),
                "Coastal reward declares next-Dock reduction");
        HexCoordinate dockPosition = validNeighbour(fixture.state, fixture.tribe.getCampCoordinate(), null);
        Building dock = new Building(dockPosition, Constants.BuildingType.DOCK);
        fixture.state.getPlayer().addBuilding(dock);
        fixture.state.getMap().getHex(dockPosition).setHasBuilding(true);
        mission.refreshCompletionState();
        require(fixture.state.turnInMission(fixture.tribe) == MissionActionResult.SUCCESS,
                "Coastal mission claims");
        require(fixture.state.getDiscountedDockBuilds() == 1, "one future Dock is discounted");
        assertAmount(fixture.state.getBuildCost(Constants.BuildingType.DOCK), 0, 10, 5, 0,
                "discounted Dock cost");
        fixture.state.onBuildingConstructed(new Building(dockPosition, Constants.BuildingType.DOCK));
        require(fixture.state.getDiscountedDockBuilds() == 0, "Dock discount is consumed once");
        assertAmount(fixture.state.getBuildCost(Constants.BuildingType.DOCK), 0, 20, 10, 0,
                "normal Dock cost restored");
    }

    private static void failedMissionCooldownIsFiveTurns() {
        MissionFixture fixture = new MissionFixture(TribeType.MOUNTAIN);
        TribeMission mission = fixture.accept();
        for (int i = 0; i < mission.getTotalTurns(); i++) fixture.state.endTurn();
        require(fixture.state.getMission(fixture.tribe) == null, "failed mission leaves active slot");
        fixture.tribe.getRelation().setScore(20);
        require(fixture.state.requestMission(fixture.tribe) == MissionActionResult.COOLDOWN,
                "failure immediately starts cooldown");
        for (int i = 0; i < 4; i++) fixture.state.endTurn();
        fixture.tribe.getRelation().setScore(20);
        require(fixture.state.requestMission(fixture.tribe) == MissionActionResult.COOLDOWN,
                "failure remains blocked through the fifth cooldown turn");
        fixture.state.endTurn();
        fixture.tribe.getRelation().setScore(20);
        require(fixture.state.requestMission(fixture.tribe) == MissionActionResult.SUCCESS,
                "mission becomes available after five cooldown turns");
    }

    private static int countMilitary(GameState state) {
        int count = 0;
        for (model.Unit unit : state.getPlayer().getUnits()) if (unit instanceof MilitaryUnit) count++;
        return count;
    }

    private static HexCoordinate validNeighbour(GameState state, HexCoordinate center,
                                                HexCoordinate excluded) {
        for (HexCoordinate coordinate : center.findNeighbours()) {
            if (state.getMap().containsCoordinate(coordinate)
                    && (excluded == null || !coordinate.equals(excluded))) return coordinate;
        }
        throw new AssertionError("no valid neighbour for " + center);
    }

    private static void assertAmount(ResourceAmount amount, int food, int wood, int stone, int iron,
                                     String message) {
        require(amount.get(Constants.ResourceType.FOOD) == food
                        && amount.get(Constants.ResourceType.WOOD) == wood
                        && amount.get(Constants.ResourceType.STONE) == stone
                        && amount.get(Constants.ResourceType.IRON) == iron,
                message + " has incorrect resources");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class MissionFixture {
        private final GameState state = new GameState(15, 13, 113L);
        private final Tribe tribe;

        private MissionFixture(TribeType type) {
            tribe = find(state, type);
            tribe.discover();
            tribe.getRelation().setScore(20);
        }

        private TribeMission accept() {
            require(state.requestMission(tribe) == MissionActionResult.SUCCESS, "mission accepts");
            return state.getMission(tribe);
        }
    }

    private static Tribe find(GameState state, TribeType type) {
        for (Tribe tribe : state.getTribes()) if (tribe.getType() == type) return tribe;
        throw new AssertionError("missing " + type);
    }
}
