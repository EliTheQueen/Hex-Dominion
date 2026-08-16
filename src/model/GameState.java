package model;

import java.util.ArrayList;
import java.util.List;
import model.season.SeasonCycle;
import model.disaster.DisasterGenerator;
import model.disaster.DisasterOccurrencePolicy;
import model.disaster.DisasterOriginSelector;
import model.disaster.DisasterSelector;
import model.disaster.DisasterEvent;

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

    public GameState(int mapWidth, int mapHeight) {
        MapGenerator gen = new MapGenerator();
        map = gen.generateMap(mapWidth, mapHeight);

        player = new Player("Player");
        this.seasonCycle = new SeasonCycle();

        random = new Random();

        disasterGenerator = new DisasterGenerator(
                new DisasterOccurrencePolicy(),
                new DisasterSelector(),
                new DisasterOriginSelector()
        );

        HexCoordinate center = new HexCoordinate(mapWidth / 2, mapHeight / 2);
        this.townHallPos = center;

        Building townHall = new Building(center, Constants.BuildingType.TOWN_HALL);
        player.addBuilding(townHall);
        Hex centerHex = map.getHex(center);
        if (centerHex != null) centerHex.setHasBuilding(true);

        // Initial territory: center hex and its in-bounds neighbours.
        player.expandTerritory(center);
        for (HexCoordinate n : center.findNeighbours()) {
            if (map.containsCoordinate(n)) {
                player.expandTerritory(n);
            }
        }

        spawnStartingUnits(center);

        updateVisibility();

        currentTurn = 1;
        gameOver = false;
        gameOverReason = "";
        finalScore = 0;
        starving = false;
    }

    /** Doc start: 1 Explorer, 2 Builders, 2 Workers placed around the Town Hall. */
    private void spawnStartingUnits(HexCoordinate center) {
        List<HexCoordinate> spots = new ArrayList<>();
        for (HexCoordinate n : center.findNeighbours()) {
            if (map.containsCoordinate(n)) spots.add(n);
        }
        int i = 0;
        player.addUnit(new Explorer(spots.get(i++ % spots.size())));
        player.addUnit(new Builder(spots.get(i++ % spots.size())));
        player.addUnit(new Builder(spots.get(i++ % spots.size())));
        player.addUnit(new Worker(spots.get(i++ % spots.size())));
        player.addUnit(new Worker(spots.get(i++ % spots.size())));
    }

    public void endTurn() {
        if (gameOver) return;
        lastTurnEvents.clear();
        boolean profTools = player.hasProfessionalTools();

        // 1. Production from buildings, depleting the underlying hex resource.
        for (Building b : player.getBuildings()) {
            if (!b.isActive())
                continue;
            if (b.getType() == Constants.BuildingType.TOWN_HALL) {
                player.addResources(ResourceAmount.of(Constants.TOWN_HALL_FOOD, Constants.TOWN_HALL_WOOD, 0, 0));
                continue;
            }
            ResourceAmount yield = b.produce(profTools);
            yield = depleteForYield(b, yield);
            player.addResources(yield);
        }

        for (Building building : player.getBuildings()) {
            building.advanceTurnStatus();
        }

        // 2. Town Hall production queue advances one step (frozen during starvation).
        if (!starving) {
            ProductionTask done = player.getProductionQueue().advance();
            if (done != null) completeTask(done);
        }

        for (Building b : player.getBuildings()) {
            if (!b.isActive() || b.getType() == Constants.BuildingType.TOWN_HALL)
                continue;
            ResourceAmount upkeep = b.getUpkeepCost();
            if (upkeep.get(Constants.ResourceType.FOOD) == 0 && upkeep.get(Constants.ResourceType.WOOD) == 0
                    && upkeep.get(Constants.ResourceType.STONE) == 0 && upkeep.get(Constants.ResourceType.IRON) == 0) {
                continue;
            }
            if (player.canAfford(upkeep)) {
                player.spend(upkeep);
                b.payUpkeep();
            } else if (b.missUpkeep()) {
                Hex h = map.getHex(b.getPosition());
                if (h != null) h.setHasBuilding(false);
                lastTurnEvents.add(b.getType().name() + " fell into ruin (unpaid upkeep)");
            }
        }

        // 4. Each living unit eats food; a shortfall triggers a starvation crisis.
        int unitCount = player.getUnitCount();
        int shortage = player.getResources().forceSpendFood(unitCount * Constants.FOOD_PER_UNIT);
        starving = shortage > 0;
        if (starving) lastTurnEvents.add("STARVATION: food ran out");

        // 5. Refresh action points (reduced while starving) and clear movement flags.
        for (Unit u : player.getUnits()) {
            if (!u.isAlive()) continue;
            u.resetAP();
            if (starving) {
                int reduced = (int) Math.floor(u.getMaxAP() * Constants.STARVATION_AP_FACTOR);
                u.setCurrentAP(Math.max(1, reduced));
            }
            if (u.getState() == Constants.UnitState.MOVING) {
                u.setState(Constants.UnitState.IDLE);
            }
        }

        // 6. Auto-explore for explorers in auto mode.
        for (Unit u : player.getUnits()) {
            if (u.isAlive() && u instanceof Explorer && ((Explorer) u).isAutoExploreMode()) {
                autoExplore((Explorer) u);
            }
        }

        player.removeDeadUnits();

        // 7. Reveal hexes around units/buildings.
        updateVisibility();

        // 8. Advance turn. This is an open-ended sandbox: there is no turn limit and no
        // "victory". The only terminal state is losing every unit and building.
        currentTurn++;
        seasonCycle.advanceTurn();

        DisasterEvent disaster = disasterGenerator.generate(
                seasonCycle.getCurrentSeason(),
                false,
                map,
                player,
                random
        );

        if (disaster != null) {
            disaster.start();

            lastTurnEvents.add("Disaster: " + disaster.getType().name());

            disaster.complete();
        }

        if (player.getUnitCount() == 0 && player.getActiveBuildingCount() == 0) {
            gameOver = true;
            gameOverReason = "All units and buildings lost";
            finalScore = ScoreCalculator.calculate(player, map);
        }
    }

    /** Consumes the hex's natural resource to back a yield; releases workers once it runs dry. */
    private ResourceAmount depleteForYield(Building b, ResourceAmount yield) {
        Constants.NaturalResourceType nat = harvestResource(b);
        if (nat == Constants.NaturalResourceType.NONE) return yield;
        Hex hex = map.getHex(b.getPosition());
        if (hex == null) return yield;

        int produced = yield.get(Constants.PRODUCES.get(b.getType()));
        int available = hex.getNaturalResourceAmount(nat);
        if (available <= 0) return ResourceAmount.zero();

        int actual = Math.min(produced, available);
        hex.decreaseNaturalResource(nat, actual);

        if (hex.isResourceDepleted(nat)) {
            // Resource exhausted: stationed workers become idle, awaiting reassignment.
            for (Worker w : new ArrayList<>(b.getWorkers())) {
                w.unstation();
            }
            lastTurnEvents.add(b.getType().name() + " exhausted its resource");
        }

        ResourceAmount result = ResourceAmount.zero();
        result.set(Constants.PRODUCES.get(b.getType()), actual);
        return result;
    }

    /** Which natural resource a producing building draws from its hex. */
    private Constants.NaturalResourceType harvestResource(Building b) {
        Hex hex = map.getHex(b.getPosition());
        switch (b.getType()) {
            case LUMBER_MILL: return Constants.NaturalResourceType.WOOD;
            case STONE_MINE:  return Constants.NaturalResourceType.STONE;
            case IRON_MINE:   return Constants.NaturalResourceType.IRON;
            case FARM:
                if (hex != null && hex.getNaturalResourceAmount(Constants.NaturalResourceType.WHEAT) > 0)
                    return Constants.NaturalResourceType.WHEAT;
                return Constants.NaturalResourceType.RICE;
            case STABLE:
                if (hex != null && hex.getNaturalResourceAmount(Constants.NaturalResourceType.COW) > 0)
                    return Constants.NaturalResourceType.COW;
                return Constants.NaturalResourceType.SHEEP;
            default: return Constants.NaturalResourceType.NONE;
        }
    }

    private void completeTask(ProductionTask task) {
        if (task.getKind() == ProductionTask.Kind.TECH) {
            player.applyTech(task.getTechType());
            lastTurnEvents.add("Researched " + task.getTechType().name());
        } else {
            HexCoordinate pos = findFreeHexNearTownHall();
            if (pos == null) {
                // No room: re-queue the finished unit so progress is not lost.
                player.getProductionQueue().getTasks().add(0, ProductionTask.forUnit(task.getUnitType()));
                return;
            }
            Unit unit;
            switch (task.getUnitType()) {
                case EXPLORER:        unit = new Explorer(pos); break;
                case BUILDER:         unit = new Builder(pos); break;
                case WORKER:          unit = new Worker(pos); break;
                case BORDER_EXPANDER: unit = new BorderExpander(pos); break;
                default: return;
            }
            player.addUnit(unit);
            lastTurnEvents.add("Trained " + task.getUnitType().name());
        }
    }

    public HexCoordinate findFreeHexNearTownHall() {
        if (player.getUnitAt(townHallPos) == null) return townHallPos;
        for (HexCoordinate n : townHallPos.findNeighbours()) {
            if (map.containsCoordinate(n) && player.getUnitAt(n) == null) return n;
        }
        // Widen the search ring if the inner ring is full.
        for (Hex h : map.getHexesInRadius(townHallPos, 2)) {
            HexCoordinate c = h.getCoordinate();
            if (player.getUnitAt(c) == null) return c;
        }
        return null;
    }

    /** Validates whether a building of {@code type} may be placed on {@code coord}. */
    public boolean canBuildAt(HexCoordinate coord, Constants.BuildingType type) {
        Hex hex = map.getHex(coord);
        if (hex == null) return false;
        if (!hex.getIsExplored()) return false;
        if (!player.isInTerritory(coord)) return false;
        if (player.getBuildingAt(coord) != null || hex.getHasBuilding()) return false;

        switch (type) {
            case LUMBER_MILL:
                return hex.hasResource(Constants.NaturalResourceType.WOOD);
            case STONE_MINE:
                return player.hasResearched(Constants.TechnologyType.STONE_MINING)
                        && hex.hasResource(Constants.NaturalResourceType.STONE);
            case IRON_MINE:
                return player.hasResearched(Constants.TechnologyType.IRON_MINING)
                        && hex.hasResource(Constants.NaturalResourceType.IRON);
            case FARM:
                return hex.hasResource(Constants.NaturalResourceType.WHEAT)
                        || hex.hasResource(Constants.NaturalResourceType.RICE);
            case STABLE:
                return hex.hasResource(Constants.NaturalResourceType.COW)
                        || hex.hasResource(Constants.NaturalResourceType.SHEEP);
            case TOWNSHIP:
                return player.hasResearched(Constants.TechnologyType.TOWNSHIP)
                        && !hex.everHadResource();
            default:
                return false;
        }
    }

    /** Projected net change of each resource next turn (may be negative), by ResourceType ordinal. */
    public int[] getNetRate() {
        int[] net = new int[Constants.ResourceType.values().length];
        boolean profTools = player.hasProfessionalTools();

        net[Constants.ResourceType.FOOD.ordinal()] += Constants.TOWN_HALL_FOOD;
        net[Constants.ResourceType.WOOD.ordinal()] += Constants.TOWN_HALL_WOOD;

        for (Building b : player.getBuildings()) {
            if (!b.isActive() || b.getType() == Constants.BuildingType.TOWN_HALL) continue;
            ResourceAmount yield = b.produce(profTools);
            Constants.NaturalResourceType nat = harvestResource(b);
            Hex hex = map.getHex(b.getPosition());
            if (nat != Constants.NaturalResourceType.NONE && hex != null) {
                int avail = hex.getNaturalResourceAmount(nat);
                Constants.ResourceType out = Constants.PRODUCES.get(b.getType());
                int produced = Math.min(yield.get(out), avail);
                net[out.ordinal()] += produced;
            }
            ResourceAmount up = b.getUpkeepCost();
            for (Constants.ResourceType r : Constants.ResourceType.values()) net[r.ordinal()] -= up.get(r);
        }
        // Food eaten by units.
        net[Constants.ResourceType.FOOD.ordinal()] -= player.getUnitCount() * Constants.FOOD_PER_UNIT;
        return net;
    }

    public List<Unit> getIdleUnitsWithAP() {
        List<Unit> idle = new ArrayList<>();
        for (Unit u : player.getUnits()) {
            if (!u.isAlive()) continue;
            if (u.getState() == Constants.UnitState.STATIONED) continue;
            if (u instanceof Explorer && ((Explorer) u).isAutoExploreMode()) continue;
            if (u.getCurrentAP() > 0) idle.add(u);
        }
        return idle;
    }

    /** Greedily walks an auto-explore unit toward the nearest unexplored frontier hex. */
    private void autoExplore(Explorer e) {
        while (e.getCurrentAP() > 0) {
            HexCoordinate target = nearestUnexploredFrontier(e.getPosition());
            if (target == null) break;
            List<HexCoordinate> path = PathFinder.findPath(map, e.getPosition(), target, e.getCurrentAP());
            if (path == null || path.size() < 2) break;
            HexCoordinate next = path.get(1);
            // Don't strand the unit deep in fog without a way back inside this turn.
            if (!e.moveTo(map, next)) break;
            updateVisibility();
        }
    }

    /** Nearest explored hex that borders unexplored territory (the exploration frontier). */
    private HexCoordinate nearestUnexploredFrontier(HexCoordinate from) {
        HexCoordinate best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Hex h : map.getAllHexes()) {
            if (!h.getIsExplored()) continue;
            boolean bordersFog = false;
            for (HexCoordinate n : h.getCoordinate().findNeighbours()) {
                Hex nh = map.getHex(n);
                if (nh != null && !nh.getIsExplored()) { bordersFog = true; break; }
            }
            if (!bordersFog) continue;
            int d = from.distanceTo(h.getCoordinate());
            if (d > 0 && d < bestDist) { bestDist = d; best = h.getCoordinate(); }
        }
        return best;
    }

    /** Recomputes fog of war from current unit and building positions. */
    public void updateVisibility() {
        for (Hex h : map.getAllHexes()) {
            if (h.isVisible()) h.setVisible(false);
        }
        for (Unit u : player.getUnits()) {
            if (!u.isAlive()) continue;
            for (Hex h : map.getHexesInRadius(u.getPosition(), u.getVisionRadius())) {
                h.setVisible(true);
            }
        }
        for (Building b : player.getBuildings()) {
            if (!b.isActive()) continue;
            int radius = b.getType() == Constants.BuildingType.TOWN_HALL ? 2 : Constants.BUILDING_VISION;
            for (Hex h : map.getHexesInRadius(b.getPosition(), radius)) {
                h.setVisible(true);
            }
        }
    }

    public GameMap getMap() { return map; }
    public Player getPlayer() { return player; }
    public HexCoordinate getTownHallPos() { return townHallPos; }
    public int getCurrentTurn() { return currentTurn; }
    public boolean isGameOver() { return gameOver; }
    public boolean isStarving() { return starving; }
    public String getGameOverReason() { return gameOverReason; }
    public int getFinalScore() { return finalScore; }
    public List<String> getLastTurnEvents() { return lastTurnEvents; }
    public SeasonCycle getSeasonCycle() {return seasonCycle;}
    public int getCurrentScore() { return ScoreCalculator.calculate(player, map); }
}
