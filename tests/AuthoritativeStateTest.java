import java.util.HashMap;
import java.util.Map;

import model.Building;
import model.Constants;
import model.GameMap;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Worker;
import model.combat.CombatRequest;
import model.combat.CombatService;
import model.combat.DiceRoller;
import model.military.MilitaryDamageHandler;
import model.military.MilitaryHex;
import model.military.Swordsman;
import model.townhall.CommandStartResult;
import model.townhall.TownHallLevel;

public final class AuthoritativeStateTest {
    public static void main(String[] args) {
        destructionReleasesEveryReference();
        structureCombatUsesDestructionLifecycle();
        townHallAggregateOwnsProjectionAndStorage();
        System.out.println("AuthoritativeStateTest passed");
    }

    private static void destructionReleasesEveryReference() {
        HexCoordinate coordinate = new HexCoordinate(0, 0);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        Hex hex = new Hex(coordinate, Constants.TerrainType.PLAIN);
        cells.put(coordinate, hex);
        GameMap map = new GameMap(cells);
        Player player = new Player("test");

        Building farm = new Building(coordinate, Constants.BuildingType.FARM);
        player.addBuilding(farm);
        hex.setHasBuilding(true);
        Worker first = new Worker(coordinate);
        Worker second = new Worker(coordinate);
        player.addUnit(first);
        player.addUnit(second);
        require(first.station(farm) && second.station(farm), "workers can station before destruction");

        require(player.destroyBuilding(map, farm), "owned building is destroyed");
        require(!player.getBuildings().contains(farm), "destroyed building removed from player");
        require(player.getBuildingAt(coordinate) == null, "destroyed building is not addressable");
        require(!hex.getHasBuilding() && hex.canHoldBuilding(), "destroyed building frees its hex");
        require(!first.isStationed() && !second.isStationed(), "all workers are released");
        require(farm.getWorkers().isEmpty(), "building retains no worker references");
        require(!player.destroyBuilding(map, farm), "destruction is idempotent");
    }

    private static void structureCombatUsesDestructionLifecycle() {
        HexCoordinate targetPosition = new HexCoordinate(0, 0);
        HexCoordinate attackerPosition = new HexCoordinate(1, 0);
        Map<HexCoordinate, Hex> cells = new HashMap<>();
        Hex targetHex = new Hex(targetPosition, Constants.TerrainType.PLAIN);
        cells.put(targetPosition, targetHex);
        cells.put(attackerPosition, new Hex(attackerPosition, Constants.TerrainType.PLAIN));
        GameMap map = new GameMap(cells);
        Player player = new Player("test");
        Building farm = new Building(targetPosition, Constants.BuildingType.FARM);
        farm.synchronizeHealth(10, 100);
        player.addBuilding(farm);
        targetHex.setHasBuilding(true);

        Swordsman attacker = new Swordsman(attackerPosition);
        player.addUnit(attacker);
        MilitaryHex attackers = new MilitaryHex(map.getHex(attackerPosition));
        attackers.addUnit(attacker);
        int damage = new CombatService(map, player, new DiceRoller(), new MilitaryDamageHandler())
                .resolve(CombatRequest.building(attacker, attackers, farm, null))
                .getStructureDamage();
        require(damage == 10, "structure attack applies fixed damage");
        require(player.getBuildingAt(targetPosition) == null, "combat removes destroyed building");
        require(!targetHex.getHasBuilding(), "combat clears structure occupancy");
    }

    private static void townHallAggregateOwnsProjectionAndStorage() {
        GameState state = new GameState(15, 13);
        Building projection = state.getPlayer().getBuildingAt(state.getTownHallPos());
        require(projection != null && projection.getTownHallLevel() == TownHallLevel.BASE_CAMP,
                "map Town Hall is bound to the authoritative aggregate");
        projection.takeDamage(10);
        require(state.getTownHall().getCurrentHp() == 190, "projection damage updates aggregate");
        state.getTownHall().takeDamage(5);
        require(projection.getCurrentHp() == 185, "projection reads aggregate health");
        require(state.getPlayer().getResources().getCap(Constants.ResourceType.FOOD) == 100,
                "Base Camp storage is 100");

        state.getPlayer().addResources(model.ResourceAmount.of(0, 100, 100, 0));
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED, "Settlement upgrade starts");
        for (int turn = 0; turn < 3; turn++) state.endTurn();
        require(state.getTownHall().getLevel() == TownHallLevel.SETTLEMENT,
                "Settlement upgrade completes");
        require(projection.getTownHallLevel() == TownHallLevel.SETTLEMENT,
                "projection level follows aggregate");
        assertStorageCapacity(state, 200);

        state.getPlayer().addResources(model.ResourceAmount.of(0, 0, 100, 100));
        require(state.startTownHallUpgrade() == CommandStartResult.STARTED, "Capital upgrade starts");
        for (int turn = 0; turn < 5; turn++) state.endTurn();
        require(state.getTownHall().getLevel() == TownHallLevel.CAPITAL,
                "Capital upgrade completes");
        require(projection.getTownHallLevel() == TownHallLevel.CAPITAL,
                "projection exposes Capital level");
        assertStorageCapacity(state, 350);
    }

    private static void assertStorageCapacity(GameState state, int expected) {
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            require(state.getPlayer().getResources().getCap(type) == expected,
                    type + " capacity should be " + expected);
        }
        require(state.getTownHall().getStorageCapacity() == expected,
                "Town Hall and ResourceStorage capacity agree");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
