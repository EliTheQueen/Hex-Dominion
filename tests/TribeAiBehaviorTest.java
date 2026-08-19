import model.Constants;
import model.GameState;
import model.Hex;
import model.HexCoordinate;
import model.MovementPolicy;
import model.PathFinder;
import model.ResourceAmount;
import model.Unit;
import model.combat.CombatReport;
import model.combat.CombatTargetType;
import model.military.MilitaryUnitType;
import model.military.Swordsman;
import model.tribe.DiplomacyResult;
import model.tribe.Tribe;
import model.tribe.TribeMilitaryUnit;
import model.tribe.TribeNotification;
import model.tribe.TribeNotificationType;
import model.tribe.TribeType;
import model.tribe.mission.MissionActionResult;

import java.util.EnumSet;
import java.util.Set;

public final class TribeAiBehaviorTest {
    public static void main(String[] args) {
        guardsAreTypedPositionedMilitaryUnits();
        playerAttackUsesTypedGuardCombat();
        neutralWarningAndAcceleratedDispleasedPenaltyWork();
        friendlyMissionAndTradeOffersAreRealState();
        enemyGuardSpawningUsesExactCadenceAndCaps();
        campDefenseMovesAndAttacksThroughCombatEngine();
        campDefeatCreatesOutpostTerritoryLootAndCleanup();
        System.out.println("TribeAiBehaviorTest passed");
    }

    private static void guardsAreTypedPositionedMilitaryUnits() {
        GameState state = new GameState(15, 13, 130L);
        for (Tribe tribe : state.getTribes()) {
            require(tribe.getGuardCount() == 2, tribe.getType() + " starts with two typed guards");
            Set<MilitaryUnitType> categories = EnumSet.noneOf(MilitaryUnitType.class);
            for (TribeMilitaryUnit unit : tribe.getMilitaryUnits()) {
                require(unit.getPosition().equals(tribe.getCampCoordinate()), "guard starts at its camp");
                require(unit.getCurrentAP() > 0 && unit.getRange() >= 1, "guard has AP and range");
                require(unit.getMilitaryUnitType() != MilitaryUnitType.CATAPULT,
                        "guard maps only to Sword/Archer/Cavalry categories");
                categories.add(unit.getMilitaryUnitType());
            }
            require(!categories.isEmpty(), "typed category is present");
        }
    }

    private static void playerAttackUsesTypedGuardCombat() {
        GameState state = new GameState(15, 13, 131L);
        Tribe farmer = find(state, TribeType.FARMER);
        farmer.discover();
        Swordsman attacker = new Swordsman(validNeighbour(state, farmer.getCampCoordinate()));
        state.getPlayer().addUnit(attacker);
        int apBefore = attacker.getCurrentAP();
        CombatReport report = state.attackTribeCamp(attacker, farmer);
        require(report != null && report.getTargetType() == CombatTargetType.TRIBE_GUARDS,
                "player attack targets a typed tribe military hex");
        require(!report.getDefenderRolls().isEmpty(), "typed guard categories contribute defense dice");
        require(attacker.getCurrentAP() == apBefore - 1 || !attacker.isAlive(),
                "authoritative guard combat consumes attacker AP");
    }

    private static void neutralWarningAndAcceleratedDispleasedPenaltyWork() {
        GameState state = new GameState(15, 13, 132L);
        Tribe farmer = find(state, TribeType.FARMER);
        farmer.discover();
        Unit intruder = state.getPlayer().getUnits().get(0);
        intruder.setPosition(validNeighbour(state, farmer.getCampCoordinate()));
        int before = farmer.getRelation().getScore();
        state.endTurn();
        require(farmer.getRelation().getScore() == before, "first forbidden-zone turn only warns");
        require(state.getForbiddenZoneIntrusionTurns(farmer) == 1, "warning starts proximity tracking");
        require(lastNotification(state).getType() == TribeNotificationType.FORBIDDEN_ZONE_WARNING,
                "warning creates a typed notification");

        state.endTurn();
        require(farmer.getRelation().getScore() == before - 2,
                "continued neutral trespass reduces relation next turn");
        farmer.getRelation().setScore(-25);
        state.endTurn();
        require(farmer.getRelation().getScore() == -29,
                "Displeased proximity degrades twice as fast");
    }

    private static void friendlyMissionAndTradeOffersAreRealState() {
        GameState state = new GameState(15, 13, 133L);
        Tribe merchant = find(state, TribeType.MERCHANT);
        merchant.discover(); merchant.getRelation().setScore(20);
        for (int i = 0; i < 4; i++) state.endTurn();
        require(state.getCurrentTurn() == 5, "offer test reaches turn five");
        require(state.getOfferedMission(merchant) != null, "turn-five mission offer is stored");
        require(hasNotification(state, merchant, TribeNotificationType.MISSION_OFFER),
                "mission offer notifies the player");
        require(state.requestMission(merchant) == MissionActionResult.SUCCESS,
                "player can accept the offered mission");
        require(state.getOfferedMission(merchant) == null, "accepting consumes the offer");

        state.endTurn();
        require(state.hasTradeOffer(merchant), "friendly trade offer is active for the current turn");
        require(hasNotification(state, merchant, TribeNotificationType.TRADE_OFFER),
                "trade offer notifies the player");
    }

    private static void enemyGuardSpawningUsesExactCadenceAndCaps() {
        GameState normalState = new GameState(15, 13, 134L);
        Tribe farmer = find(normalState, TribeType.FARMER);
        farmer.discover(); farmer.getRelation().becomeEnemy();
        normalState.endTurn();
        require(farmer.getGuardCount() == 2, "no guard spawns before turn three");
        normalState.endTurn();
        require(farmer.getGuardCount() == 3, "normal tribe spawns on turn three");
        for (int i = 0; i < 6; i++) normalState.endTurn();
        require(farmer.getGuardCount() == 3, "normal tribe guard cap is three");

        GameState warriorState = new GameState(15, 13, 135L);
        Tribe warrior = find(warriorState, TribeType.WARRIOR);
        warrior.discover(); warrior.getRelation().becomeEnemy();
        for (int i = 0; i < 11; i++) warriorState.endTurn();
        require(warrior.getGuardCount() == 5, "Warrior tribe spawns every three turns up to five");
        Set<MilitaryUnitType> categories = EnumSet.noneOf(MilitaryUnitType.class);
        for (TribeMilitaryUnit unit : warrior.getMilitaryUnits()) categories.add(unit.getMilitaryUnitType());
        require(categories.contains(MilitaryUnitType.SWORDSMAN)
                        && categories.contains(MilitaryUnitType.ARCHER)
                        && categories.contains(MilitaryUnitType.CAVALRY),
                "Warrior roster maps into all required combat categories");
    }

    private static void campDefenseMovesAndAttacksThroughCombatEngine() {
        GameState state = new GameState(15, 13, 136L);
        Tribe farmer = find(state, TribeType.FARMER);
        farmer.discover(); farmer.getRelation().becomeEnemy();
        TribeMilitaryUnit defender = farmer.getMilitaryUnits().get(0);
        HexCoordinate targetPosition = findTwoStepTarget(state, farmer.getCampCoordinate());
        Swordsman target = new Swordsman(targetPosition);
        state.getPlayer().addUnit(target);
        HexCoordinate originalPosition = defender.getPosition();
        farmer.recordCampAttack(targetPosition);

        state.endTurn();
        require(!defender.getPosition().equals(originalPosition),
                "nearest defender moves toward the attacker");
        require(defender.getCurrentAP() < defender.getMaxAP(),
                "defense movement/attack consumes real AP");
        CombatReport report = state.getLastTribeCombatReport();
        require(report != null && report.getTargetType() == CombatTargetType.MILITARY_UNITS,
                "tribe attack is resolved by the authoritative combat engine");
        require(hasNotification(state, farmer, TribeNotificationType.MILITARY_ACTION),
                "defensive action creates a notification");
    }

    private static void campDefeatCreatesOutpostTerritoryLootAndCleanup() {
        GameState state = new GameState(15, 13, 137L);
        Tribe farmer = find(state, TribeType.FARMER);
        farmer.discover(); farmer.getRelation().setScore(20);
        require(state.requestMission(farmer) == MissionActionResult.SUCCESS, "pre-defeat mission exists");
        farmer.getRelation().setScore(70);
        require(state.requestAlliance(farmer) == DiplomacyResult.SUCCESS, "pre-defeat alliance exists");
        farmer.removeGuards(99);
        farmer.takeDamage(farmer.getCurrentHp() - 1);
        int foodBefore = state.getPlayer().getResources().get(Constants.ResourceType.FOOD);
        Swordsman attacker = new Swordsman(validNeighbour(state, farmer.getCampCoordinate()));
        state.getPlayer().addUnit(attacker);

        CombatReport report = state.attackTribeCamp(attacker, farmer);
        require(report != null && farmer.isDefeated(), "camp is defeated through structure combat");
        require(farmer.isOutpost(), "defeated camp converts to Outpost");
        require(farmer.getGuardCount() == 0, "defeat disbands tribe military");
        require(state.getPlayer().isInTerritory(farmer.getCampCoordinate()),
                "Outpost camp territory transfers to player");
        for (Hex hex : state.getMap().getHexesInRadius(farmer.getCampCoordinate(), 1)) {
            if (hex.getTerrainType() != Constants.TerrainType.SEA
                    && hex.getTerrainType() != Constants.TerrainType.MOUNTAIN_RANGE) {
                require(state.getPlayer().isInTerritory(hex.getCoordinate()),
                        "eligible adjacent Outpost territory transfers");
            }
        }
        require(state.getPlayer().getResources().get(Constants.ResourceType.FOOD) == foodBefore + 30,
                "Farmer camp grants type-specific loot");
        require(!state.isAllied(farmer) && state.getMission(farmer) == null,
                "defeat clears alliance and mission state");
        require(hasNotification(state, farmer, TribeNotificationType.CAMP_DEFEATED),
                "camp defeat creates a notification");

        require(!sameAmount(state.getTribeDefeatLoot(TribeType.FARMER),
                        state.getTribeDefeatLoot(TribeType.WARRIOR)),
                "different tribe types expose different loot");
    }

    private static HexCoordinate findTwoStepTarget(GameState state, HexCoordinate camp) {
        for (HexCoordinate step : PathFinder.reachable(state.getMap(), camp, 1, MovementPolicy.basic())) {
            for (HexCoordinate target : step.findNeighbours()) {
                if (state.getMap().containsCoordinate(target) && camp.distanceTo(target) == 2
                        && state.getPlayer().getUnitAt(target) == null) return target;
            }
        }
        throw new AssertionError("no two-step defense target");
    }

    private static HexCoordinate validNeighbour(GameState state, HexCoordinate center) {
        for (HexCoordinate coordinate : center.findNeighbours()) {
            if (state.getMap().containsCoordinate(coordinate)) return coordinate;
        }
        throw new AssertionError("no valid neighbour for " + center);
    }

    private static TribeNotification lastNotification(GameState state) {
        require(!state.getTribeNotifications().isEmpty(), "notification exists");
        return state.getTribeNotifications().get(state.getTribeNotifications().size() - 1);
    }

    private static boolean hasNotification(GameState state, Tribe tribe, TribeNotificationType type) {
        for (TribeNotification notification : state.getTribeNotifications()) {
            if (notification.getTribeId().equals(tribe.getId()) && notification.getType() == type) return true;
        }
        return false;
    }

    private static boolean sameAmount(ResourceAmount first, ResourceAmount second) {
        for (Constants.ResourceType type : Constants.ResourceType.values()) {
            if (first.get(type) != second.get(type)) return false;
        }
        return true;
    }

    private static Tribe find(GameState state, TribeType type) {
        for (Tribe tribe : state.getTribes()) if (tribe.getType() == type) return tribe;
        throw new AssertionError("missing " + type);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
