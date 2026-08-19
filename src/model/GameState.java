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

/** Central game state: holds the map, the player and the turn-by-turn progression. */
public class GameState {

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
    private int lastTradeTurn = -1;

    private final AdjacencyBonusService adjacencyBonusService;

    private final HappinessTracker happinessTracker;
    private final HappinessService happinessService;
    private boolean militaryCapPenaltyApplied;

    private final MilitaryRecruitmentService militaryRecruitmentService;

    private final TownHall townHall;
    private final TownHallCommandService townHallCommandService;
    private final TechnologyRegistry phaseTwoTechnologies;
    private final TechnologyResearchService technologyResearchService;
    private final PhaseTwoTechnologyTarget phaseTwoTechnologyTarget;

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

        if (bearAttackCooldown > 0) {
            bearAttackCooldown--;
        }

        boolean professionalTools = player.hasProfessionalTools() || hasSteelTools();

        applyRecurringHappiness();

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

        // 7. Visibility.
        updateVisibility();

        map.advanceBlockedHexes();

        // 8. Advance turn and season.
        currentTurn++;
        seasonCycle.advanceTurn();

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
                disaster.start();

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

        Hex hex = map.getHex(building.getPosition());

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

                return Constants.NaturalResourceType.SHEEP;

            default:
                return Constants.NaturalResourceType.NONE;
        }
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
                return hex.hasResource(
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
                return map.isCoastal(coord);

            case MONUMENT:
                return !player.hasBuildingType(
                        Constants.BuildingType.MONUMENT
                )
                        && !hex.everHadResource();

            case BAZAAR:
                return !player.hasBuildingType(
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

            Hex hex =
                    map.getHex(building.getPosition());

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
    }

    public boolean tradeAtBazaar(
            Constants.ResourceType sell,
            Constants.ResourceType buy,
            int quantitySold
    ) {
        if (!canTradeThisTurn()) {
            return false;
        }

        Building bazaar = getBazaar();

        if (bazaar == null) {
            return false;
        }

        BazaarTradePolicy policy =
                new BazaarTradePolicy(
                        bazaar.getBazaarTradeLevel()
                );

        boolean successful =
                tradeService.complete(
                        player,
                        policy,
                        sell,
                        buy,
                        quantitySold
                );

        if (successful) {
            markTradeUsed();
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

    public boolean tradeAtTradingPost(
            Constants.ResourceType sell,
            Constants.ResourceType buy,
            int quantitySold
    ) {
        if (!canTradeThisTurn()) {
            return false;
        }

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
            markTradeUsed();
        }

        return successful;
    }

    public boolean canTradeThisTurn() {
        return lastTradeTurn != currentTurn;
    }

    private void markTradeUsed() {
        lastTradeTurn = currentTurn;
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
            for (HexCoordinate neighbour : townHallPos.findNeighbours()) {
                if (map.containsCoordinate(neighbour)) {
                    infrastructureService.buildAutomaticWall(townHallPos, neighbour);
                }
            }
        }
        public boolean isDefensiveArchitectureEnabled() { return defensiveArchitecture; }
    }
}
