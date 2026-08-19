package model;

import model.disaster.*;
import model.happiness.HappinessEventType;
import model.happiness.HappinessLevel;
import model.happiness.HappinessModifiers;
import model.happiness.HappinessService;
import model.happiness.HappinessTracker;
import model.military.MilitaryRecruitmentService;
import model.military.MilitaryUnit;
import model.military.MilitaryUnitType;
import model.season.SeasonCycle;
import model.season.SeasonProductionModifiers;
import model.townhall.TownHallLevel;
import model.townhall.TownHall;
import model.townhall.TownHallCommandService;
import model.townhall.UpgradeTownHallCommand;
import model.townhall.CommandStartResult;
import model.technology.*;
import model.trade.BazaarTradePolicy;
import model.trade.TradeService;
import model.trade.TradingPostPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.tribe.*;
import model.tribe.mission.*;
import model.tribe.behavior.*;
import model.combat.CombatReport;
import model.military.MilitaryDamageHandler;
import model.military.MilitaryHex;

/** Central game state: holds the map, the player and the turn-by-turn progression. */
public class GameState implements java.io.Serializable {

    private final GameMap map;
    private final Player player;
    private final HexCoordinate townHallPos;

    private int currentTurn;
    private boolean gameOver;
    private String gameOverReason;
    private int finalScore;

    private final SeasonCycle seasonCycle;

    private boolean starving;
    private final List<String> lastTurnEvents = new ArrayList<>();

    private final Random random;

    private final DisasterGenerator disasterGenerator;
    private DisasterEvent lastDisasterEvent;

    private BearAttackEvent activeBearAttack;
    private int bearAttackCooldown = 0;

    private final InfrastructureService infrastructureService;

    private final TradeService tradeService;
    private final AdjacencyBonusService adjacencyBonusService;

    private final HappinessTracker happinessTracker;
    private final HappinessService happinessService;
    private boolean militaryCapPenaltyApplied;
    private final java.util.Set<MilitaryUnit> rewardedTownHallGarrisons = new java.util.HashSet<>();

    private final MilitaryRecruitmentService militaryRecruitmentService;

    private final TownHall townHall;
    private final TownHallCommandService townHallCommandService;
    private final TechnologyRegistry phaseTwoTechnologies;
    private final TechnologyResearchService technologyResearchService;
    private final PhaseTwoTechnologyTarget phaseTwoTechnologyTarget;
    private final List<Tribe> tribes;
    private final TribeMissionService tribeMissionService;
    private final TribeGiftService tribeGiftService;
    private final TribeTradeService tribeTradeService;
    private final TribeAllianceRegistry tribeAllianceRegistry;
    private final TribeAllianceService tribeAllianceService;
    private final TribeWarService tribeWarService;
    private final TribePeaceService tribePeaceService;
    private final TribeTurnService tribeTurnService;
    private final java.util.Map<String, Integer> tribeMissionCooldownUntil = new java.util.HashMap<>();

    public GameState(int mapWidth, int mapHeight) {
        MapGenerator generator = new MapGenerator();
        map = generator.generateMap(mapWidth, mapHeight);

        player = new Player("Player");

        seasonCycle = new SeasonCycle();

        infrastructureService = new InfrastructureService(map, player);

        random = new Random();

        disasterGenerator = new DisasterGenerator(
                new DisasterOccurrencePolicy(),
                new DisasterSelector(),
                new DisasterOriginSelector()
        );

        HexCoordinate center = new HexCoordinate(mapWidth / 2, mapHeight / 2);
        townHallPos = center;

        Building townHallBuilding = new Building(center, Constants.BuildingType.TOWN_HALL);
        player.addBuilding(townHallBuilding);

        Hex centerHex = map.getHex(center);

        if (centerHex != null) {
            centerHex.setHasBuilding(true);
        }

        player.expandTerritory(center);

        for (HexCoordinate neighbour : center.findNeighbours()) {
            if (map.containsCoordinate(neighbour)) {
                player.expandTerritory(neighbour);
            }
        }

        spawnStartingUnits(center);
        updateVisibility();

        currentTurn = 1;
        gameOver = false;
        gameOverReason = "";
        finalScore = 0;
        starving = false;

        tradeService = new TradeService();

        adjacencyBonusService = new AdjacencyBonusService();

        happinessTracker = new HappinessTracker();
        happinessService = new HappinessService(happinessTracker);
        militaryCapPenaltyApplied = false;

        militaryRecruitmentService = new MilitaryRecruitmentService(player, map);

        townHall = new TownHall();
        townHallCommandService = new TownHallCommandService(townHall, player.getResources());
        phaseTwoTechnologies = new TechnologyRegistry();
        phaseTwoTechnologyTarget = new PhaseTwoTechnologyTarget();
        TechnologyEffectService effects = new TechnologyEffectService(
                java.util.Arrays.asList(new SailingEffect(), new SteelToolsEffect(),
                        new DefensiveArchitectureEffect()));
        technologyResearchService = new TechnologyResearchService(
                townHall, phaseTwoTechnologies, effects, phaseTwoTechnologyTarget,
                townHallCommandService);
        tribes = createTribes();
        tribeMissionService = new TribeMissionService(player);
        tribeGiftService = new TribeGiftService();
        tribeTradeService = new TribeTradeService(tradeService, new TribeTradePolicyFactory());
        tribeAllianceRegistry = new TribeAllianceRegistry();
        tribeAllianceService = new TribeAllianceService(tribeAllianceRegistry);
        tribeWarService = new TribeWarService();
        tribePeaceService = new TribePeaceService();
        tribeTurnService = new TribeTurnService(new DefaultTribeBehaviorStrategy());
        updateVisibility();
    }

    /** Doc start: 1 Explorer, 2 Builders, 2 Workers placed around the Town Hall. */
    private void spawnStartingUnits(HexCoordinate center) {
        List<HexCoordinate> spots = new ArrayList<>();

        for (HexCoordinate neighbour : center.findNeighbours()) {
            if (map.containsCoordinate(neighbour)) {
                spots.add(neighbour);
            }
        }

        int i = 0;

        player.addUnit(new Explorer(spots.get(i++ % spots.size())));
        player.addUnit(new Builder(spots.get(i++ % spots.size())));
        player.addUnit(new Builder(spots.get(i++ % spots.size())));
        player.addUnit(new Worker(spots.get(i++ % spots.size())));
        player.addUnit(new Worker(spots.get(i++ % spots.size())));
    }

    public void endTurn() {
        if (gameOver) {
            return;
        }

        lastTurnEvents.clear();

        townHallCommandService.advanceOneTurn();
        syncTownHallBuildingFromDomain();

        if (bearAttackCooldown > 0) {
            bearAttackCooldown--;
        }

        boolean professionalTools = player.hasProfessionalTools() || hasSteelTools();

        applyRecurringHappiness();
        applyTownHallGarrisonHappiness();

        HappinessLevel happinessLevel = happinessService.getCurrentLevel();

        // 1. Production.
        for (Building building : player.getBuildings()) {
            if (!building.isActive()) {
                continue;
            }

            if (building.getType() == Constants.BuildingType.TOWN_HALL) {
                player.addResources(
                        ResourceAmount.of(
                                Constants.TOWN_HALL_FOOD,
                                Constants.TOWN_HALL_WOOD,
                                0,
                                0
                        )
                );

                continue;
            }

            ResourceAmount yield = building.produce(professionalTools);

            Constants.ResourceType producedResource =
                    Constants.PRODUCES.get(building.getType());

            int adjacencyBonus =
                    adjacencyBonusService.calculateBonus(building, player, map);

            if (producedResource != null) {
                int production =
                        yield.get(producedResource) + adjacencyBonus;

                production = SeasonProductionModifiers.apply(
                        production, seasonCycle.getCurrentSeason(), building.getType());

                int modifiedProduction =
                        HappinessModifiers.applyProductionModifiers(
                                production,
                                building.getWorkers().size(),
                                happinessLevel
                        );

                yield.set(producedResource, modifiedProduction);
            }

            yield = depleteForYield(building, yield);

            player.addResources(yield);
        }

        for (Building building : player.getBuildings()) {
            building.advanceTurnStatus();
        }

        // 2. Production queue.
        if (!starving) {
            ProductionTask completedTask =
                    player.getProductionQueue().advance();

            if (completedTask != null) {
                completeTask(completedTask);
            }
        }

        // 3. Building upkeep.
        for (Building building : player.getBuildings()) {
            if (!building.isActive()
                    || building.getType() == Constants.BuildingType.TOWN_HALL) {

                continue;
            }

            ResourceAmount upkeep = building.getUpkeepCost();

            boolean noUpkeep =
                    upkeep.get(Constants.ResourceType.FOOD) == 0
                            && upkeep.get(Constants.ResourceType.WOOD) == 0
                            && upkeep.get(Constants.ResourceType.STONE) == 0
                            && upkeep.get(Constants.ResourceType.IRON) == 0;

            if (noUpkeep) {
                continue;
            }

            if (player.canAfford(upkeep)) {
                player.spend(upkeep);
                building.payUpkeep();
            } else if (building.missUpkeep()) {
                Hex hex = map.getHex(building.getPosition());

                if (hex != null) {
                    hex.setHasBuilding(false);
                }

                lastTurnEvents.add(
                        building.getType().name()
                                + " fell into ruin (unpaid upkeep)"
                );
            }
        }

        // 4. Food consumption and starvation.
        int unitCount = player.getUnitCount();

        int shortage =
                player.getResources().forceSpendFood(
                        unitCount * Constants.FOOD_PER_UNIT
                );

        starving = shortage > 0;

        if (starving) {
            lastTurnEvents.add("STARVATION: food ran out");
        }

        // 5. Reset AP and apply penalties.
        int happinessPenalty =
                HappinessModifiers.actionPointPenalty(happinessLevel);

        for (Unit unit : player.getUnits()) {
            if (!unit.isAlive()) {
                continue;
            }

            unit.resetAP();

            if (starving) {
                int reduced =
                        (int) Math.floor(
                                unit.getMaxAP()
                                        * Constants.STARVATION_AP_FACTOR
                        );

                unit.setCurrentAP(Math.max(1, reduced));
            }

            if (happinessPenalty > 0) {
                unit.setCurrentAP(
                        Math.max(
                                0,
                                unit.getCurrentAP() - happinessPenalty
                        )
                );
            }

            if (unit.getState() == Constants.UnitState.MOVING) {
                unit.setState(Constants.UnitState.IDLE);
            }
        }

        // 6. Auto explore.
        for (Unit unit : player.getUnits()) {
            if (unit.isAlive()
                    && unit instanceof Explorer
                    && ((Explorer) unit).isAutoExploreMode()) {

                autoExplore((Explorer) unit);
            }
        }

        player.removeDeadUnits();
        rewardedTownHallGarrisons.removeIf(unit -> !unit.isAlive());
        if (getMilitaryUnitCount() < getMilitaryUnitCap(townHall.getLevel())) militaryCapPenaltyApplied = false;

        // 7. Visibility.
        updateVisibility();

        map.advanceBlockedHexes();

        // 8. Advance turn and season.
        currentTurn++;
        seasonCycle.advanceTurn();

        java.util.List<Tribe> missionFailures = tribeMissionService.advanceOneTurn();
        for (Tribe failedTribe : missionFailures) {
            happinessService.applyEvent(HappinessEventType.MISSION_FAILED);
            tribeMissionCooldownUntil.put(failedTribe.getId(), currentTurn + 3);
        }
        processTribeTurns();

        // 9. Active bear event.
        if (activeBearAttack != null) {
            activeBearAttack.processTurn();

            if (activeBearAttack.shouldEnd()) {
                activeBearAttack.complete();

                lastTurnEvents.add("Bear attack ended");

                activeBearAttack = null;
            }
        }

        // 10. Disaster generation.
        if (activeBearAttack == null) {
            DisasterEvent disaster =
                    disasterGenerator.generate(
                            seasonCycle.getCurrentSeason(),
                            false,
                            bearAttackCooldown == 0,
                            map,
                            player,
                            random
                    );

            if (disaster != null) {
                Building hallBuilding = player.getBuildingAt(townHallPos);
                int hallHpBefore = hallBuilding == null ? townHall.getCurrentHp() : hallBuilding.getCurrentHp();
                disaster.start();

                if (hallBuilding != null && hallBuilding.getCurrentHp() < hallHpBefore) {
                    townHall.takeDamage(hallHpBefore - hallBuilding.getCurrentHp());
                }
                syncTownHallBuildingFromDomain();

                lastDisasterEvent = disaster;

                lastTurnEvents.add(
                        "Disaster: " + disaster.getType().name()
                );

                if (disaster instanceof BearAttackEvent) {
                    activeBearAttack = (BearAttackEvent) disaster;
                    bearAttackCooldown = 5;
                } else {
                    disaster.complete();
                }
            }
        }

        if (player.getUnitCount() == 0
                && player.getActiveBuildingCount() == 0) {

            gameOver = true;
            gameOverReason = "All units and buildings lost";
            finalScore = ScoreCalculator.calculate(player, map);
        }
    }

    private void applyRecurringHappiness() {
        for (Building building : player.getBuildings()) {
            if (!building.isActive()) {
                continue;
            }

            if (building.getType() == Constants.BuildingType.MONUMENT) {
                happinessService.applyEvent(
                        HappinessEventType.MONUMENT_ACTIVATED
                );
            }
        }
    }

    private void applyTownHallGarrisonHappiness() {
        for (Unit unit : player.getUnits()) {
            if (unit instanceof MilitaryUnit && unit.isAlive() && unit.getPosition().equals(townHallPos)
                    && rewardedTownHallGarrisons.add((MilitaryUnit) unit))
                happinessService.applyEvent(HappinessEventType.TOWN_HALL_GARRISONED);
        }
    }

    public void onBuildingConstructed(Building building) {
        if (building == null) {
            return;
        }

        if (building.getType() == Constants.BuildingType.TOWNSHIP) {
            happinessService.applyEvent(
                    HappinessEventType.TOWNSHIP_BUILT
            );
        }
    }

    public void onMilitaryCapReached() {
        if (militaryCapPenaltyApplied) {
            return;
        }

        happinessService.applyEvent(
                HappinessEventType.UNIT_CAP_REACHED
        );

        militaryCapPenaltyApplied = true;
    }

    /** Consumes natural resources to back the calculated production. */
    private ResourceAmount depleteForYield(
            Building building,
            ResourceAmount yield
    ) {
        Constants.NaturalResourceType naturalResource =
                harvestResource(building);

        if (naturalResource == Constants.NaturalResourceType.NONE) {
            return yield;
        }

        Hex hex = findHarvestHex(building, naturalResource);

        if (hex == null) {
            return yield;
        }

        Constants.ResourceType producedResource =
                Constants.PRODUCES.get(building.getType());

        if (producedResource == null) {
            return yield;
        }

        int produced = yield.get(producedResource);

        int available =
                hex.getNaturalResourceAmount(naturalResource);

        if (available <= 0) {
            return ResourceAmount.zero();
        }

        int actual = Math.min(produced, available);

        hex.decreaseNaturalResource(
                naturalResource,
                actual
        );

        if (hex.isResourceDepleted(naturalResource)) {
            for (Worker worker :
                    new ArrayList<>(building.getWorkers())) {

                worker.unstation();
            }

            lastTurnEvents.add(
                    building.getType().name()
                            + " exhausted its resource"
            );
        }

        ResourceAmount result = ResourceAmount.zero();

        result.set(producedResource, actual);

        return result;
    }

    /** Returns the natural resource consumed by a producing building. */
    private Constants.NaturalResourceType harvestResource(
            Building building
    ) {
        Hex hex = map.getHex(building.getPosition());

        switch (building.getType()) {
            case LUMBER_MILL:
                return Constants.NaturalResourceType.WOOD;

            case STONE_MINE:
                return Constants.NaturalResourceType.STONE;

            case IRON_MINE:
                return Constants.NaturalResourceType.IRON;

            case FARM:
                if (hex != null
                        && hex.getNaturalResourceAmount(
                        Constants.NaturalResourceType.WHEAT
                ) > 0) {

                    return Constants.NaturalResourceType.WHEAT;
                }

                return Constants.NaturalResourceType.RICE;

            case STABLE:
                if (hex != null
                        && hex.getNaturalResourceAmount(
                        Constants.NaturalResourceType.COW
                ) > 0) {

                    return Constants.NaturalResourceType.COW;
                }

                if (hex != null && hex.getNaturalResourceAmount(Constants.NaturalResourceType.SHEEP) > 0)
                    return Constants.NaturalResourceType.SHEEP;
                return Constants.NaturalResourceType.NONE;

            case DOCK:
                return Constants.NaturalResourceType.FISH;

            default:
                return Constants.NaturalResourceType.NONE;
        }
    }

    private Hex findHarvestHex(Building building, Constants.NaturalResourceType resource) {
        Hex own = map.getHex(building.getPosition());
        if (own != null && own.getNaturalResourceAmount(resource) > 0) return own;
        if (building.getType() == Constants.BuildingType.DOCK) {
            for (Hex neighbour : map.getNeighboursOf(building.getPosition()))
                if (neighbour.getNaturalResourceAmount(resource) > 0) return neighbour;
        }
        return own;
    }

    private void completeTask(ProductionTask task) {
        if (task.getKind() == ProductionTask.Kind.TECH) {
            player.applyTech(task.getTechType());

            lastTurnEvents.add(
                    "Researched " + task.getTechType().name()
            );

            return;
        }

        HexCoordinate position =
                findFreeHexNearTownHall();

        if (position == null) {
            player.getProductionQueue()
                    .getTasks()
                    .add(
                            0,
                            ProductionTask.forUnit(
                                    task.getUnitType()
                            )
                    );

            return;
        }

        Unit unit;

        switch (task.getUnitType()) {
            case EXPLORER:
                unit = new Explorer(position);
                break;

            case BUILDER:
                unit = new Builder(position);
                break;

            case WORKER:
                unit = new Worker(position);
                break;

            case BORDER_EXPANDER:
                unit = new BorderExpander(position);
                break;

            default:
                return;
        }

        player.addUnit(unit);

        lastTurnEvents.add(
                "Trained " + task.getUnitType().name()
        );
    }

    public HexCoordinate findFreeHexNearTownHall() {
        if (player.getUnitAt(townHallPos) == null) {
            return townHallPos;
        }

        for (HexCoordinate neighbour :
                townHallPos.findNeighbours()) {

            if (map.containsCoordinate(neighbour)
                    && player.getUnitAt(neighbour) == null) {

                return neighbour;
            }
        }

        for (Hex hex :
                map.getHexesInRadius(townHallPos, 2)) {

            HexCoordinate coordinate =
                    hex.getCoordinate();

            if (player.getUnitAt(coordinate) == null) {
                return coordinate;
            }
        }

        return null;
    }

    /** Validates whether a building may be placed on a coordinate. */
    public boolean canBuildAt(
            HexCoordinate coord,
            Constants.BuildingType type
    ) {
        Hex hex = map.getHex(coord);

        if (hex == null) {
            return false;
        }

        if (!hex.getIsExplored()) {
            return false;
        }

        if (!player.isInTerritory(coord)) {
            return false;
        }

        if (player.getBuildingAt(coord) != null
                || hex.getHasBuilding()) {

            return false;
        }

        if (hex.getTerrainType() == Constants.TerrainType.SEA
                || hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) return false;

        switch (type) {
            case LUMBER_MILL:
                return hex.hasResource(
                        Constants.NaturalResourceType.WOOD
                );

            case STONE_MINE:
                return player.hasResearched(
                        Constants.TechnologyType.STONE_MINING
                )
                        && hex.hasResource(
                        Constants.NaturalResourceType.STONE
                );

            case IRON_MINE:
                return player.hasResearched(
                        Constants.TechnologyType.IRON_MINING
                )
                        && hex.hasResource(
                        Constants.NaturalResourceType.IRON
                );

            case FARM:
                return hex.hasResource(
                        Constants.NaturalResourceType.WHEAT
                )
                        || hex.hasResource(
                        Constants.NaturalResourceType.RICE
                );

            case STABLE:
                return hex.getTerrainType() == Constants.TerrainType.PLAIN
                        || hex.hasResource(
                        Constants.NaturalResourceType.COW
                )
                        || hex.hasResource(
                        Constants.NaturalResourceType.SHEEP
                );

            case TOWNSHIP:
                return player.hasResearched(
                        Constants.TechnologyType.TOWNSHIP
                )
                        && !hex.everHadResource();

            case DOCK:
                return townHall.getLevel().getLevelNumber() >= 2 && map.isCoastal(coord);

            case MONUMENT:
                return (hex.getTerrainType() == Constants.TerrainType.PLAIN
                        || hex.getTerrainType() == Constants.TerrainType.GRASSLAND)
                        && !hex.everHadResource();

            case BAZAAR:
                return townHall.getLevel().getLevelNumber() >= 2
                        && !player.hasBuildingType(
                        Constants.BuildingType.BAZAAR
                )
                        && !hex.everHadResource();

            default:
                return false;
        }
    }

    /** Projected next-turn resource change. Does not mutate game state. */
    public int[] getNetRate() {
        int[] net =
                new int[Constants.ResourceType.values().length];

        boolean professionalTools =
                player.hasProfessionalTools() || hasSteelTools();

        HappinessLevel happinessLevel =
                happinessService.getCurrentLevel();

        net[Constants.ResourceType.FOOD.ordinal()]
                += Constants.TOWN_HALL_FOOD;

        net[Constants.ResourceType.WOOD.ordinal()]
                += Constants.TOWN_HALL_WOOD;

        for (Building building :
                player.getBuildings()) {

            if (!building.isActive()
                    || building.getType()
                    == Constants.BuildingType.TOWN_HALL) {

                continue;
            }

            ResourceAmount yield =
                    building.produce(professionalTools);

            Constants.ResourceType producedResource =
                    Constants.PRODUCES.get(
                            building.getType()
                    );

            int adjacencyBonus =
                    adjacencyBonusService.calculateBonus(
                            building,
                            player,
                            map
                    );

            if (producedResource != null) {
                int production =
                        yield.get(producedResource)
                                + adjacencyBonus;

                production = SeasonProductionModifiers.apply(
                        production, seasonCycle.getCurrentSeason(), building.getType());

                int modifiedProduction =
                        HappinessModifiers
                                .applyProductionModifiers(
                                        production,
                                        building.getWorkers().size(),
                                        happinessLevel
                                );

                yield.set(
                        producedResource,
                        modifiedProduction
                );
            }

            Constants.NaturalResourceType naturalResource =
                    harvestResource(building);

            Hex hex = findHarvestHex(building, naturalResource);

            if (producedResource != null) {
                int produced =
                        yield.get(producedResource);

                if (naturalResource
                        != Constants.NaturalResourceType.NONE
                        && hex != null) {

                    int available =
                            hex.getNaturalResourceAmount(
                                    naturalResource
                            );

                    produced =
                            Math.min(
                                    produced,
                                    available
                            );
                }

                net[producedResource.ordinal()]
                        += produced;
            }

            ResourceAmount upkeep =
                    building.getUpkeepCost();

            for (Constants.ResourceType resourceType :
                    Constants.ResourceType.values()) {

                net[resourceType.ordinal()]
                        -= upkeep.get(resourceType);
            }
        }

        net[Constants.ResourceType.FOOD.ordinal()]
                -= player.getUnitCount()
                * Constants.FOOD_PER_UNIT;

        return net;
    }

    public List<Unit> getIdleUnitsWithAP() {
        List<Unit> idle = new ArrayList<>();

        for (Unit unit : player.getUnits()) {
            if (!unit.isAlive()) {
                continue;
            }

            if (unit.getState()
                    == Constants.UnitState.STATIONED) {

                continue;
            }

            if (unit instanceof Explorer
                    && ((Explorer) unit)
                    .isAutoExploreMode()) {

                continue;
            }

            if (unit.getCurrentAP() > 0) {
                idle.add(unit);
            }
        }

        return idle;
    }

    /** Greedily walks an auto-explore unit toward the nearest unexplored frontier hex. */
    private void autoExplore(Explorer explorer) {
        while (explorer.getCurrentAP() > 0) {
            HexCoordinate target =
                    nearestUnexploredFrontier(
                            explorer.getPosition()
                    );

            if (target == null) {
                break;
            }

            List<HexCoordinate> path =
                    PathFinder.findPath(
                            map,
                            explorer.getPosition(),
                            target,
                            explorer.getCurrentAP(),
                            getMovementPolicy()
                    );

            if (path == null
                    || path.size() < 2) {

                break;
            }

            HexCoordinate next =
                    path.get(1);

            if (!explorer.moveTo(map, next, getMovementPolicy())) {
                break;
            }

            updateVisibility();
        }
    }

    /** Nearest explored hex that borders unexplored territory. */
    private HexCoordinate nearestUnexploredFrontier(
            HexCoordinate from
    ) {
        HexCoordinate best = null;

        int bestDistance =
                Integer.MAX_VALUE;

        for (Hex hex : map.getAllHexes()) {
            if (!hex.getIsExplored()) {
                continue;
            }

            boolean bordersFog = false;

            for (HexCoordinate neighbourCoordinate :
                    hex.getCoordinate()
                            .findNeighbours()) {

                Hex neighbour =
                        map.getHex(
                                neighbourCoordinate
                        );

                if (neighbour != null
                        && !neighbour.getIsExplored()) {

                    bordersFog = true;
                    break;
                }
            }

            if (!bordersFog) {
                continue;
            }

            int distance =
                    from.distanceTo(
                            hex.getCoordinate()
                    );

            if (distance > 0
                    && distance < bestDistance) {

                bestDistance = distance;
                best = hex.getCoordinate();
            }
        }

        return best;
    }

    /** Recomputes fog of war from current unit and building positions. */
    public void updateVisibility() {
        for (Hex hex : map.getAllHexes()) {
            if (hex.isVisible()) {
                hex.setVisible(false);
            }
        }

        for (Unit unit : player.getUnits()) {
            if (!unit.isAlive()) {
                continue;
            }

            for (Hex hex :
                    map.getHexesInRadius(
                            unit.getPosition(),
                            unit.getVisionRadius()
                    )) {

                hex.setVisible(true);
            }
        }

        for (Building building :
                player.getBuildings()) {

            if (!building.isActive()) {
                continue;
            }

            int radius =
                    building.getType()
                            == Constants.BuildingType.TOWN_HALL
                            ? 2
                            : Constants.BUILDING_VISION;

            for (Hex hex :
                    map.getHexesInRadius(
                            building.getPosition(),
                            radius
                    )) {

                hex.setVisible(true);
            }
        }
        if (tribes != null) {
            for (Tribe tribe : tribes) {
                Hex camp = map.getHex(tribe.getCampCoordinate());
                if (camp != null && camp.getIsExplored()) tribe.discover();
            }
        }
    }

    public boolean tradeAtBazaar(
            Constants.ResourceType sell,
            Constants.ResourceType buy,
            int quantitySold
    ) {
        Building bazaar = getBazaar();

        if (bazaar == null || !bazaar.canTradeAt(currentTurn)) {
            return false;
        }

        model.trade.BazaarTradeLevel tier = model.trade.BazaarTradeLevel.forQuantity(quantitySold);
        if (tier == null) return false;
        BazaarTradePolicy policy =
                new BazaarTradePolicy(tier);

        boolean successful =
                tradeService.complete(
                        player,
                        policy,
                        sell,
                        buy,
                        quantitySold
                );

        if (successful) {
            bazaar.markTradedAt(currentTurn);
        }

        return successful;
    }

    private Building getBazaar() {
        for (Building building :
                player.getBuildings()) {

            if (building.getType()
                    == Constants.BuildingType.BAZAAR
                    && building.isActive()) {

                return building;
            }
        }

        return null;
    }

    public boolean upgradeBazaar() {
        Building bazaar = getBazaar();

        if (bazaar == null) {
            return false;
        }

        return bazaar.upgradeBazaar();
    }

    public boolean tradeAtTradingPost(HexCoordinate tradingPost,
            Constants.ResourceType sell,
            Constants.ResourceType buy,
            int quantitySold
    ) {
        if (tradingPost == null || !map.canTradeAtPost(tradingPost, currentTurn)
                || !player.isInTerritory(tradingPost)) return false;

        TradingPostPolicy policy =
                new TradingPostPolicy();

        boolean successful =
                tradeService.complete(
                        player,
                        policy,
                        sell,
                        buy,
                        quantitySold
                );

        if (successful) {
            map.markTradingPostUsed(tradingPost, currentTurn);
        }

        return successful;
    }

    public Building getActiveBazaar() { return getBazaar(); }
    public java.util.Set<HexCoordinate> getEligibleTradingPosts() {
        java.util.Set<HexCoordinate> result = new java.util.HashSet<>();
        for (HexCoordinate coordinate : map.getTradingPosts())
            if (player.isInTerritory(coordinate)) result.add(coordinate);
        return result;
    }

    public List<Tribe> getTribes() { return java.util.Collections.unmodifiableList(tribes); }
    public TribeMission getMission(Tribe tribe) { return tribeMissionService.getActiveMission(tribe); }

    public boolean giftTribe(Tribe tribe, Constants.ResourceType resource, int amount) {
        return tribeGiftService.sendGift(player, tribe, resource, amount);
    }

    public boolean tradeWithTribe(Tribe tribe, Constants.ResourceType sell,
                                  Constants.ResourceType buy, int amount) {
        return tribeTradeService.trade(player, tribe, sell, buy, amount, currentTurn);
    }

    public MissionActionResult requestMission(Tribe tribe) {
        if (tribe == null) return MissionActionResult.INVALID_REQUEST;
        if (currentTurn < tribeMissionCooldownUntil.getOrDefault(tribe.getId(), 0)) return MissionActionResult.COOLDOWN;
        TribeMission mission = createMissionFor(tribe);
        return tribeMissionService.acceptMission(mission);
    }

    public MissionActionResult turnInMission(Tribe tribe) {
        MissionActionResult result = tribeMissionService.claimMission(tribeMissionService.getActiveMission(tribe));
        if (result == MissionActionResult.SUCCESS) tribeMissionCooldownUntil.put(tribe.getId(), currentTurn + 3);
        return result;
    }

    public MissionActionResult cancelMission(Tribe tribe) {
        MissionActionResult result = tribeMissionService.cancelMission(tribeMissionService.getActiveMission(tribe));
        if (result == MissionActionResult.SUCCESS) happinessService.applyEvent(HappinessEventType.MISSION_CANCELLED);
        if (result == MissionActionResult.SUCCESS) tribeMissionCooldownUntil.put(tribe.getId(), currentTurn + 3);
        return result;
    }

    public DiplomacyResult declareWar(Tribe tribe) {
        TribeRelationStatus before = tribe == null ? null : tribe.getRelation().getStatus();
        DiplomacyResult result = tribeWarService.declareWar(tribe);
        if (result == DiplomacyResult.SUCCESS) {
            if (before == TribeRelationStatus.ALLIED) happinessService.applyEvent(HappinessEventType.ALLIED_TRIBE_ATTACKED);
            else if (before == TribeRelationStatus.FRIENDLY) happinessService.applyEvent(HappinessEventType.FRIENDLY_TRIBE_ATTACKED);
            tribeAllianceService.breakAlliance(tribe);
            if (tribeMissionService.getActiveMission(tribe) != null) cancelMission(tribe);
        }
        return result;
    }

    public DiplomacyResult requestPeace(Tribe tribe) { return tribePeaceService.requestPeace(player, tribe); }
    public DiplomacyResult requestAlliance(Tribe tribe) { return tribeAllianceService.requestAlliance(tribe); }
    public boolean isAllied(Tribe tribe) { return tribeAllianceRegistry.isAlliedWith(tribe); }

    public Tribe getTribeAt(HexCoordinate coordinate) {
        for (Tribe tribe : tribes) if (!tribe.isDefeated() && tribe.getCampCoordinate().equals(coordinate)) return tribe;
        return null;
    }

    public CombatReport attackTribeCamp(MilitaryUnit attacker, Tribe tribe) {
        if (attacker == null || tribe == null || !tribe.isDiscovered() || tribe.isDefeated()
                || !attacker.canAttack() || attacker.getPosition().distanceTo(tribe.getCampCoordinate()) > attacker.getRange())
            return null;
        attacker.spendAttackAP();
        TribeRelationStatus before = tribe.getRelation().getStatus();
        if (before == TribeRelationStatus.ALLIED) happinessService.applyEvent(HappinessEventType.ALLIED_TRIBE_ATTACKED);
        else if (before == TribeRelationStatus.FRIENDLY) happinessService.applyEvent(HappinessEventType.FRIENDLY_TRIBE_ATTACKED);
        tribeAllianceService.breakAlliance(tribe);
        tribe.getRelation().becomeEnemy();

        if (tribe.getGuardCount() == 0) {
            int damage = attacker.getStructureDamage();
            tribe.takeDamage(damage);
            return new CombatReport(attacker.getMilitaryUnitType().name(), tribe.getName() + " CAMP",
                    java.util.Collections.emptyList(), java.util.Collections.emptyList(), 0, 0,
                    tribe.isDefeated() ? "Camp defeated" : "Camp damaged", damage);
        }

        MilitaryHex attackerHex = militaryHexAt(attacker.getPosition());
        int attackerDice = 0;
        for (MilitaryUnit unit : attackerHex.getAliveUnits()) if (unit.contributesCombatDie()) attackerDice++;
        int defenderDice = Math.min(6, tribe.getGuardCount());
        List<Integer> attackRolls = rollDice(attackerDice);
        List<Integer> defenseRolls = rollDice(defenderDice);
        int attackWins = 0, defenseWins = 0;
        for (int i = 0; i < Math.min(attackRolls.size(), defenseRolls.size()); i++) {
            if (attackRolls.get(i) > defenseRolls.get(i)) attackWins++; else defenseWins++;
        }
        int guardsLost = tribe.removeGuards(attackWins);
        if (defenseWins > 0) new MilitaryDamageHandler().applyDamage(attackerHex, defenseWins);
        player.removeDeadUnits();
        String casualty = guardsLost + " guard" + (guardsLost == 1 ? "" : "s") + " lost";
        if (defenseWins > 0) casualty += ", " + defenseWins + " attacker damage";
        return new CombatReport(attacker.getMilitaryUnitType().name(), tribe.getName() + " GUARDS",
                attackRolls, defenseRolls, attackWins, defenseWins, casualty, 0);
    }

    private MilitaryHex militaryHexAt(HexCoordinate coordinate) {
        MilitaryHex result = new MilitaryHex(map.getHex(coordinate));
        for (Unit unit : player.getUnits()) if (unit instanceof MilitaryUnit && unit.isAlive()
                && unit.getPosition().equals(coordinate)) result.addUnit((MilitaryUnit) unit);
        return result;
    }

    private List<Integer> rollDice(int count) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < count; i++) values.add(random.nextInt(6) + 1);
        values.sort(java.util.Collections.reverseOrder()); return values;
    }

    private List<Tribe> createTribes() {
        List<Tribe> result = new ArrayList<>();
        TribeType[] types = TribeType.values();
        int index = 0;
        for (Hex hex : map.getAllHexes()) {
            if (index >= types.length) break;
            if (hex.getCoordinate().distanceTo(townHallPos) < 4
                    || hex.getTerrainType() == Constants.TerrainType.SEA
                    || hex.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE
                    || map.hasTradingPost(hex.getCoordinate()) || hex.hasNaturalResource()) continue;
            TribeType type = types[index];
            result.add(new Tribe(type.name().charAt(0) + type.name().substring(1).toLowerCase()
                    + " Clan", type, hex.getCoordinate(), type == TribeType.WARRIOR ? 180 : 140));
            index++;
        }
        return result;
    }

    private TribeMission createMissionFor(Tribe tribe) {
        TribeMissionObjective objective;
        switch (tribe.getType()) {
            case FARMER:
                objective = new ResourcePaymentObjective(player, ResourceAmount.of(0, 20, 0, 0)); break;
            case WARRIOR:
                objective = new KillCountObjective(2); break;
            case MERCHANT:
                objective = new RoadConnectionObjective(map, townHallPos, tribe.getCampCoordinate()); break;
            case MOUNTAIN:
                objective = new BuildingNearCampObjective(player, tribe.getCampCoordinate(), Constants.BuildingType.STONE_MINE); break;
            case COASTAL:
                objective = new BuildingNearCampObjective(player, tribe.getCampCoordinate(), Constants.BuildingType.DOCK); break;
            default: throw new IllegalStateException("Unknown tribe type");
        }
        return new TribeMission(tribe.getType().name() + " Accord", objective.getDescription(), tribe,
                objective, new TribeMissionReward(ResourceAmount.of(15, 15, 10, 5), 12), 8);
    }

    private void processTribeTurns() {
        for (Tribe tribe : tribes) {
            TribeTurnAction action = tribeTurnService.processTurn(tribe, new TribeTurnContext(currentTurn,
                    tribe.isCampUnderAttack(), tribe.getGuardCount(), tribeMissionService.getActiveMission(tribe) != null));
            if (action == TribeTurnAction.PRODUCE_GUARD) tribe.addGuard();
            tribe.setCampUnderAttack(false);
        }
    }

    public boolean canRecruitMilitaryUnit(
            MilitaryUnitType type,
            HexCoordinate position,
            TownHallLevel townHallLevel
    ) {
        return militaryRecruitmentService.canRecruit(
                type,
                position,
                townHallLevel
        );
    }

    public MilitaryUnit recruitMilitaryUnit(
            MilitaryUnitType type,
            HexCoordinate position,
            TownHallLevel townHallLevel
    ) {
        int before =
                militaryRecruitmentService
                        .getMilitaryUnitCount();

        int cap =
                militaryRecruitmentService
                        .getCap(townHallLevel);

        MilitaryUnit unit =
                militaryRecruitmentService.recruit(
                        type,
                        position,
                        townHallLevel
                );

        if (unit == null) {
            return null;
        }

        int after =
                militaryRecruitmentService
                        .getMilitaryUnitCount();

        if (before < cap
                && after >= cap) {

            onMilitaryCapReached();
        }

        updateVisibility();

        lastTurnEvents.add(
                "Recruited "
                        + type.name()
        );

        return unit;
    }

    public CommandStartResult startMilitaryTraining(MilitaryUnitType type) {
        if (type == null) return CommandStartResult.INVALID_COMMAND;
        HexCoordinate position = townHallPos;
        if (!militaryRecruitmentService.canRecruit(type, position, townHall.getLevel()))
            return CommandStartResult.INVALID_COMMAND;
        return townHallCommandService.startCommand(new TrainingCommand(type));
    }

    public CommandStartResult startCivilianTraining(Constants.UnitType type) {
        if (type == null || type == Constants.UnitType.MILITARY || type == Constants.UnitType.BEAR)
            return CommandStartResult.INVALID_COMMAND;
        if (player.atUnitCap()) return CommandStartResult.INVALID_COMMAND;
        ResourceAmount cost = Constants.UNIT_COST.get(type);
        Integer turns = Constants.UNIT_BUILD_TURNS.get(type);
        if (cost == null || turns == null) return CommandStartResult.INVALID_COMMAND;
        return townHallCommandService.startCommand(new TrainingCommand(type, cost, turns));
    }

    public int getMilitaryUnitCount() {
        return militaryRecruitmentService
                .getMilitaryUnitCount();
    }

    public int getMilitaryUnitCap(
            TownHallLevel level
    ) {
        return militaryRecruitmentService
                .getCap(level);
    }

    public GameMap getMap() {
        return map;
    }

    public Player getPlayer() {
        return player;
    }

    public HexCoordinate getTownHallPos() {
        return townHallPos;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isStarving() {
        return starving;
    }

    public String getGameOverReason() {
        return gameOverReason;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public List<String> getLastTurnEvents() {
        return lastTurnEvents;
    }

    public SeasonCycle getSeasonCycle() {
        return seasonCycle;
    }

    public int getCurrentScore() {
        return ScoreCalculator.calculate(
                player,
                map
        );
    }

    public DisasterEvent getLastDisasterEvent() {
        return lastDisasterEvent;
    }

    public BearAttackEvent getActiveBearAttack() {
        return activeBearAttack;
    }

    public InfrastructureService getInfrastructureService() {
        return infrastructureService;
    }

    public int getHappinessScore() {
        return happinessService.getCurrentScore();
    }

    public HappinessLevel getHappinessLevel() {
        return happinessService.getCurrentLevel();
    }

    public HappinessService getHappinessService() {
        return happinessService;
    }

    public TownHall getTownHall() { return townHall; }

    public CommandStartResult startTownHallUpgrade() {
        TownHallLevel target = townHall.getLevel().next();
        if (target == null) return CommandStartResult.INVALID_COMMAND;
        ResourceAmount cost;
        int turns;
        if (target == TownHallLevel.SETTLEMENT) {
            cost = ResourceAmount.of(0, 50, 50, 0);
            turns = 3;
        } else {
            cost = ResourceAmount.of(0, 0, 100, 50);
            turns = 5;
        }
        return townHallCommandService.startCommand(
                new UpgradeTownHallCommand(townHall, cost, turns));
    }

    public ResearchStartResult startPhaseTwoResearch(TechnologyType technology) {
        return technologyResearchService.startResearch(technology);
    }

    public boolean cancelTownHallCommand() {
        return townHallCommandService.cancelActiveCommand();
    }

    public TechnologyRegistry getPhaseTwoTechnologies() { return phaseTwoTechnologies; }
    public boolean hasSailing() { return phaseTwoTechnologyTarget.isSailingEnabled(); }
    public boolean hasSteelTools() { return phaseTwoTechnologyTarget.isSteelToolsEnabled(); }
    public boolean hasDefensiveArchitecture() { return phaseTwoTechnologyTarget.isDefensiveArchitectureEnabled(); }

    public MovementPolicy getMovementPolicy() {
        return new MovementPolicy(hasSailing(), seasonCycle.getCurrentSeason());
    }

    private final class PhaseTwoTechnologyTarget implements TechnologyEffectTarget {
        private boolean sailing;
        private boolean steelTools;
        private boolean defensiveArchitecture;
        public void enableSailing() { sailing = true; }
        public boolean isSailingEnabled() { return sailing; }
        public void enableSteelTools() { steelTools = true; }
        public boolean isSteelToolsEnabled() { return steelTools; }
        public void enableDefensiveArchitecture() {
            defensiveArchitecture = true;
            townHall.enableDefensiveArchitecture();
            for (HexCoordinate neighbour : townHallPos.findNeighbours()) {
                if (map.containsCoordinate(neighbour)) {
                    infrastructureService.buildAutomaticWall(townHallPos, neighbour);
                }
            }
        }
        public boolean isDefensiveArchitectureEnabled() { return defensiveArchitecture; }
    }

    private void syncTownHallBuildingFromDomain() {
        Building building = player.getBuildingAt(townHallPos);
        if (building != null && building.getType() == Constants.BuildingType.TOWN_HALL)
            building.synchronizeHealth(townHall.getCurrentHp(), townHall.getMaxHp());
    }

    private final class TrainingCommand extends model.townhall.AbstractProductionCommand {
        private final Constants.UnitType civilianType;
        private final MilitaryUnitType militaryType;
        private TrainingCommand(MilitaryUnitType type) {
            super(type.getCost(), type.getTrainingTurns());
            this.militaryType = type; this.civilianType = null;
        }
        private TrainingCommand(Constants.UnitType type, ResourceAmount cost, int turns) {
            super(cost, turns);
            this.civilianType = type; this.militaryType = null;
        }
        @Override protected void executeEffect() {
            if (militaryType != null) {
                recruitMilitaryUnit(militaryType, townHallPos, townHall.getLevel());
                return;
            }
            HexCoordinate position = findFreeHexNearTownHall();
            if (position == null) return;
            Unit unit;
            switch (civilianType) {
                case EXPLORER: unit = new Explorer(position); break;
                case BUILDER: unit = new Builder(position); break;
                case WORKER: unit = new Worker(position); break;
                case BORDER_EXPANDER: unit = new BorderExpander(position); break;
                default: return;
            }
            player.addUnit(unit);
            lastTurnEvents.add("Trained " + civilianType.name());
        }
    }
}
