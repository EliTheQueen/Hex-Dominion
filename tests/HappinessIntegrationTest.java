import model.BorderExpander;
import model.Builder;
import model.Building;
import model.Constants;
import model.Explorer;
import model.GameState;
import model.HexCoordinate;
import model.Worker;
import model.happiness.HappinessEventType;
import model.happiness.HappinessLevel;
import model.happiness.HappinessModifiers;
import model.happiness.HappinessService;
import model.happiness.HappinessTracker;
import model.military.Swordsman;

public final class HappinessIntegrationTest {
    public static void main(String[] args) {
        modifiersPreserveGoldenAgeAndUnhappyRules();
        townHallGarrisonIsOneCurrentCondition();
        revoltTargetsOnlyWorkersAndMilitary();
        monumentRemainsRecurring();
        System.out.println("HappinessIntegrationTest passed");
    }

    private static void modifiersPreserveGoldenAgeAndUnhappyRules() {
        HappinessTracker tracker = new HappinessTracker();
        HappinessService service = new HappinessService(tracker);
        service.applyEvent(HappinessEventType.MONUMENT_ACTIVATED);
        service.applyEvent(HappinessEventType.MONUMENT_ACTIVATED);
        require(service.getCurrentScore() == 4 && service.getCurrentLevel() == HappinessLevel.GOLDEN_AGE,
                "happiness accumulates into golden age");
        require(HappinessModifiers.applyProductionModifiers(9, 1, HappinessLevel.GOLDEN_AGE) == 9,
                "golden age floors ten percent bonus");
        require(HappinessModifiers.applyProductionModifiers(5, 2, HappinessLevel.UNHAPPY) == 3,
                "unhappy worker penalty");
    }

    private static void townHallGarrisonIsOneCurrentCondition() {
        GameState state = new GameState(15, 13);
        HexCoordinate townHall = state.getTownHallPos();
        HexCoordinate away = townHall.findNeighbours().get(0);
        Swordsman first = new Swordsman(townHall);
        Swordsman second = new Swordsman(townHall);
        state.getPlayer().addUnit(first);
        state.getPlayer().addUnit(second);
        require(state.getHappinessScore() == 1, "one or more Town Hall guards contribute exactly +1");
        first.setPosition(away);
        require(state.getHappinessScore() == 1, "second guard keeps the single +1 condition");
        second.setPosition(away);
        require(state.getHappinessScore() == 0, "garrison bonus is removed when the Town Hall is empty");
        first.setPosition(townHall);
        require(state.getHappinessScore() == 1, "returning guard cannot farm another permanent point");
    }

    private static void revoltTargetsOnlyWorkersAndMilitary() {
        HexCoordinate coordinate = new HexCoordinate(0, 0);
        Worker worker = new Worker(coordinate);
        Swordsman military = new Swordsman(coordinate);
        Explorer explorer = new Explorer(coordinate);
        Builder builder = new Builder(coordinate);
        BorderExpander expander = new BorderExpander(coordinate);
        require(HappinessModifiers.actionPointPenaltyAppliesTo(HappinessLevel.REVOLT, worker),
                "Revolt penalizes Workers");
        require(HappinessModifiers.actionPointPenaltyAppliesTo(HappinessLevel.REVOLT, military),
                "Revolt penalizes military units");
        require(!HappinessModifiers.actionPointPenaltyAppliesTo(HappinessLevel.REVOLT, explorer),
                "Revolt does not penalize Explorers");
        require(!HappinessModifiers.actionPointPenaltyAppliesTo(HappinessLevel.REVOLT, builder),
                "Revolt does not penalize Builders");
        require(!HappinessModifiers.actionPointPenaltyAppliesTo(HappinessLevel.REVOLT, expander),
                "Revolt does not penalize Border Expanders");
        require(!HappinessModifiers.actionPointPenaltyAppliesTo(HappinessLevel.NORMAL, worker),
                "normal happiness has no AP penalty");
    }

    private static void monumentRemainsRecurring() {
        GameState state = new GameState(15, 13);
        HexCoordinate position = state.getTownHallPos().findNeighbours().get(0);
        Building monument = new Building(position, Constants.BuildingType.MONUMENT);
        state.getPlayer().addBuilding(monument);
        state.getMap().getHex(position).setHasBuilding(true);
        state.endTurn();
        require(state.getHappinessScore() == 2, "active Monument grants +2 each turn");
        state.endTurn();
        require(state.getHappinessScore() == 4, "Monument reward recurs");
        state.getPlayer().destroyBuilding(state.getMap(), monument);
        state.endTurn();
        require(state.getHappinessScore() == 4, "destroyed Monument stops recurring happiness");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
