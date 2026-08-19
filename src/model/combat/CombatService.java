package model.combat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import model.Building;
import model.Constants;
import model.GameMap;
import model.Hex;
import model.HexCoordinate;
import model.Player;
import model.Unit;
import model.Wall;
import model.disaster.Bear;
import model.military.MilitaryDamageHandler;
import model.military.MilitaryHex;
import model.military.MilitaryUnit;
import model.military.MilitaryUnitType;
import model.tribe.Tribe;

/** One gameplay-authoritative resolver for unit, wildlife and structure combat. */
public final class CombatService implements Serializable {
    private static final int BEAR_DAMAGE_PER_WIN = 40;

    private final GameMap map;
    private final Player player;
    private final DiceRoller diceRoller;
    private final MilitaryDamageHandler damageHandler;

    public CombatService(GameMap map, Player player, DiceRoller diceRoller,
                         MilitaryDamageHandler damageHandler) {
        if (map == null || player == null || diceRoller == null || damageHandler == null) {
            throw new IllegalArgumentException("combat dependencies must not be null");
        }
        this.map = map;
        this.player = player;
        this.diceRoller = diceRoller;
        this.damageHandler = damageHandler;
    }

    public CombatReport resolve(CombatRequest request) {
        validateBaseRequest(request);
        return switch (request.getTargetType()) {
            case MILITARY_UNITS -> resolveMilitary(request);
            case TRIBE_GUARDS -> resolveTribeGuards(request);
            case WILD_ANIMAL -> resolveBear(request);
            case WILD_ANIMAL_ATTACK -> resolveBearAttack(request);
            case CIVILIAN -> resolveCivilian(request);
            case BUILDING -> resolveBuilding(request);
            case WALL -> resolveWall(request);
            case TRIBE_CAMP -> resolveTribeCamp(request);
            case EMPTY_HEX -> resolveCapture(request);
        };
    }

    private CombatReport resolveMilitary(CombatRequest request) {
        MilitaryHex defenders = requireDefenders(request);
        List<MilitaryUnit> participants = unitCombatParticipants(request);
        int attackDice = distinctDice(participants);
        int defenseDice = distinctDice(defenders.getAliveUnits());
        DiceBattle battle = rollBattle(attackDice, defenseDice, defendingWall(request));
        spendAttackAp(participants);
        damageHandler.applyDamage(defenders, battle.result.getAttackerWins());
        damageHandler.applyDamage(request.getAttackerHex(), battle.result.getDefenderWins());
        cleanupDeadUnits(request);
        return report(request, participants, battle, "Military casualties resolved", 0, false);
    }

    private CombatReport resolveTribeGuards(CombatRequest request) {
        Tribe tribe = request.getTribe();
        MilitaryHex defenders = request.getDefenderMilitaryHex();
        if (tribe == null || tribe.isDefeated() || defenders == null
                || defenders.getAliveUnits().isEmpty()) {
            throw new IllegalArgumentException("tribe has no defending guards");
        }
        List<MilitaryUnit> participants = unitCombatParticipants(request);
        int guardsBefore = defenders.getAliveUnits().size();
        DiceBattle battle = rollBattle(distinctDice(participants),
                distinctDice(defenders.getAliveUnits()), defendingWall(request));
        spendAttackAp(participants);
        damageHandler.applyDamage(defenders, battle.result.getAttackerWins());
        damageHandler.applyDamage(request.getAttackerHex(), battle.result.getDefenderWins());
        cleanupDeadUnits(request);
        int guardsLost = Math.max(0, guardsBefore - defenders.getAliveUnits().size());
        String casualty = guardsLost + " guard" + (guardsLost == 1 ? "" : "s") + " lost";
        if (battle.result.getDefenderWins() > 0) {
            casualty += ", " + battle.result.getDefenderWins() + " attacker damage";
        }
        return report(request, participants, battle, casualty, 0, false);
    }

    private CombatReport resolveBear(CombatRequest request) {
        Bear bear = request.getBear();
        if (bear == null || !bear.isAlive()) throw new IllegalArgumentException("bear is not alive");
        List<MilitaryUnit> participants = unitCombatParticipants(request);
        DiceBattle battle = rollBattle(distinctDice(participants), 1, null);
        spendAttackAp(participants);
        bear.takeDamage(battle.result.getAttackerWins() * BEAR_DAMAGE_PER_WIN);
        damageHandler.applyDamage(request.getAttackerHex(), battle.result.getDefenderWins());
        cleanupDeadUnits(request);
        String casualty = !bear.isAlive() ? "Bear defeated"
                : battle.result.getDefenderWins() > 0 ? "Attacker wounded" : "No effective hit";
        return report(request, participants, battle, casualty, 0, false);
    }

    private CombatReport resolveBearAttack(CombatRequest request) {
        Bear bear = request.getAttackingBear();
        Unit target = request.getCivilian();
        if (bear == null || !bear.isAlive() || target == null || !target.isAlive()
                || bear.getCurrentAP() < 1 || bear.getPosition().distanceTo(target.getPosition()) != 1) {
            throw new IllegalArgumentException("bear cannot attack target");
        }
        MilitaryHex defenders = request.getDefenderMilitaryHex();
        if (target instanceof MilitaryUnit) {
            if (defenders == null || !defenders.getAliveUnits().contains(target)) {
                throw new IllegalArgumentException("military target is not on defender hex");
            }
        }

        // Wild animals always roll one attack die and the player always rolls
        // exactly two defense dice, regardless of the defending unit mix.
        DiceBattle battle = rollBattle(1, 2, defendingWall(request));
        bear.spendAP(1);
        if (battle.result.getAttackerWins() > 0) {
            if (target instanceof MilitaryUnit) {
                damageHandler.applyHitPointDamage(defenders, bear.getAttackDamage());
            } else {
                target.takeDamage(bear.getAttackDamage());
            }
        }
        if (battle.result.getDefenderWins() > 0) {
            bear.takeDamage(battle.result.getDefenderWins() * BEAR_DAMAGE_PER_WIN);
        }
        cleanupDeadUnits(request);
        String casualty = target.isAlive()
                ? (battle.result.getAttackerWins() > 0 ? "Defender damaged" : "Attack repelled")
                : "Defender defeated";
        return new CombatReport("BEAR", target.getUnitType().name(), battle.attack.getRolls(),
                battle.defense.getRolls(), battle.result.getAttackerWins(),
                battle.result.getDefenderWins(), casualty, 0, 1,
                CombatTargetType.WILD_ANIMAL_ATTACK, battle.wallActive, false);
    }

    private CombatReport resolveCivilian(CombatRequest request) {
        Unit civilian = request.getCivilian();
        if (civilian == null || !civilian.isAlive() || civilian instanceof MilitaryUnit) {
            throw new IllegalArgumentException("target is not a living civilian");
        }
        List<MilitaryUnit> participants = unitCombatParticipants(request);
        spendAttackAp(participants);
        civilian.kill();
        cleanupDeadUnits(request);
        return directReport(request, participants, "Civilian defeated", 0, false);
    }

    private CombatReport resolveBuilding(CombatRequest request) {
        Building target = request.getBuilding();
        if (target == null || !target.isActive()) throw new IllegalArgumentException("building is not active");
        ensureNoDefenders(request.getDefenderMilitaryHex());
        List<MilitaryUnit> participants = structureParticipants(request);
        int damage = totalStructureDamage(participants);
        spendAttackAp(participants);
        target.takeDamage(damage);
        if (!target.isActive()) player.destroyBuilding(map, target);
        return directReport(request, participants, "Structure hit for " + damage, damage, false);
    }

    private CombatReport resolveWall(CombatRequest request) {
        Wall target = request.getWallTarget();
        if (target == null || target.isDestroyed()) throw new IllegalArgumentException("wall is destroyed");
        ensureNoDefenders(request.getDefenderMilitaryHex());
        List<MilitaryUnit> participants = structureParticipants(request);
        int damage = totalStructureDamage(participants);
        spendAttackAp(participants);
        target.takeDamage(damage);
        if (target.isDestroyed()) {
            map.removeWall(request.getAttackerHex().getCoordinate(), request.getDefenderCoordinate());
        }
        return directReport(request, participants, "Wall hit for " + damage, damage, false);
    }

    private CombatReport resolveTribeCamp(CombatRequest request) {
        Tribe tribe = request.getTribe();
        if (tribe == null || tribe.isDefeated()) throw new IllegalArgumentException("tribe camp is defeated");
        if (!tribe.getMilitaryUnitsAt(tribe.getCampCoordinate()).isEmpty()) {
            throw new IllegalArgumentException("guards block direct camp targeting");
        }
        List<MilitaryUnit> participants = structureParticipants(request);
        int damage = totalStructureDamage(participants);
        spendAttackAp(participants);
        tribe.takeDamage(damage);
        String result = tribe.isDefeated() ? "Camp defeated" : "Camp damaged";
        return directReport(request, participants, result, damage, false);
    }

    private CombatReport resolveCapture(CombatRequest request) {
        if (request.getAttackerHex().getCoordinate().distanceTo(request.getDefenderCoordinate()) != 1) {
            throw new IllegalArgumentException("only an adjacent empty hex can be captured");
        }
        if (player.getUnitAt(request.getDefenderCoordinate()) != null
                || player.getBuildingAt(request.getDefenderCoordinate()) != null) {
            throw new IllegalArgumentException("defender hex is not empty");
        }
        Hex destination = map.getHex(request.getDefenderCoordinate());
        if (destination == null || destination.isBlocked()
                || destination.getTerrainType() == Constants.TerrainType.SEA
                || destination.getTerrainType() == Constants.TerrainType.MOUNTAIN_RANGE) {
            throw new IllegalArgumentException("defender terrain cannot be captured");
        }
        List<MilitaryUnit> participants = adjacentParticipants(request);
        spendAttackAp(participants);
        for (MilitaryUnit unit : participants) unit.setPosition(request.getDefenderCoordinate());
        player.expandTerritory(request.getDefenderCoordinate());
        return directReport(request, participants, "Empty hex captured", 0, true);
    }

    private List<MilitaryUnit> unitCombatParticipants(CombatRequest request) {
        int distance = distance(request);
        if (distance < 1 || distance > 2) throw new IllegalArgumentException("target is out of combat range");
        List<MilitaryUnit> participants = new ArrayList<>();
        for (MilitaryUnit unit : request.getAttackerHex().getAliveUnits()) {
            if (!unit.canAttack()) continue;
            if (distance == 1 || unit.getMilitaryUnitType() == MilitaryUnitType.ARCHER) participants.add(unit);
        }
        requireInitiatorAndParticipants(request, participants);
        return participants;
    }

    private List<MilitaryUnit> structureParticipants(CombatRequest request) {
        int distance = distance(request);
        if (distance < 1 || distance > 2) throw new IllegalArgumentException("structure is out of range");
        List<MilitaryUnit> participants = new ArrayList<>();
        for (MilitaryUnit unit : request.getAttackerHex().getAliveUnits()) {
            if (unit.canAttack() && unit.getRange() >= distance) participants.add(unit);
        }
        requireInitiatorAndParticipants(request, participants);
        return participants;
    }

    private List<MilitaryUnit> adjacentParticipants(CombatRequest request) {
        List<MilitaryUnit> participants = new ArrayList<>();
        for (MilitaryUnit unit : request.getAttackerHex().getAliveUnits()) {
            if (unit.canAttack()) participants.add(unit);
        }
        requireInitiatorAndParticipants(request, participants);
        return participants;
    }

    private void requireInitiatorAndParticipants(CombatRequest request, List<MilitaryUnit> participants) {
        if (!participants.contains(request.getInitiator())) {
            throw new IllegalArgumentException("initiator is not an eligible participating attacker");
        }
        if (participants.isEmpty()) throw new IllegalArgumentException("attacker hex has no eligible force");
    }

    private int distinctDice(List<MilitaryUnit> units) {
        Set<MilitaryUnitType> types = EnumSet.noneOf(MilitaryUnitType.class);
        for (MilitaryUnit unit : units) if (unit.contributesCombatDie()) types.add(unit.getMilitaryUnitType());
        return types.size();
    }

    private int totalStructureDamage(List<MilitaryUnit> participants) {
        int damage = 0;
        for (MilitaryUnit unit : participants) damage += unit.getStructureDamage();
        return damage;
    }

    private DiceBattle rollBattle(int attackerDice, int defenderDice, Wall defendingWall) {
        DiceResult attack = diceRoller.rollD6(attackerDice);
        DiceResult defense = diceRoller.rollD6(defenderDice);
        boolean wallActive = defendingWall != null && !defendingWall.isDestroyed();
        if (wallActive) defense = addWallModifier(defense);
        CombatResolver resolver = new CombatResolver(attack, defense);
        return new DiceBattle(resolver.getAttackerDiceResult(), resolver.getDefenderDiceResult(),
                resolver.resolve(), wallActive);
    }

    private DiceResult addWallModifier(DiceResult rolls) {
        List<Integer> modified = new ArrayList<>();
        for (int roll : rolls.getRolls()) modified.add(Math.min(6, roll + 2));
        return new DiceResult(modified);
    }

    private Wall defendingWall(CombatRequest request) {
        if (distance(request) != 1) return null;
        return map.getWallBetween(attackerCoordinate(request), request.getDefenderCoordinate());
    }

    private MilitaryHex requireDefenders(CombatRequest request) {
        MilitaryHex defenders = request.getDefenderMilitaryHex();
        if (defenders == null || defenders.getAliveUnits().isEmpty()) {
            throw new IllegalArgumentException("defender hex has no military units");
        }
        return defenders;
    }

    private void ensureNoDefenders(MilitaryHex defenders) {
        if (defenders != null && !defenders.getAliveUnits().isEmpty()) {
            throw new IllegalArgumentException("defending military units block structure targeting");
        }
    }

    private void validateBaseRequest(CombatRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        if (request.getTargetType() == CombatTargetType.WILD_ANIMAL_ATTACK) {
            if (request.getAttackingBear() == null || !request.getAttackingBear().isAlive()) {
                throw new IllegalArgumentException("attacking bear is not alive");
            }
            if (map.getHex(request.getDefenderCoordinate()) == null) {
                throw new IllegalArgumentException("defender coordinate is outside the map");
            }
            return;
        }
        if (!request.getAttackerHex().getUnits().contains(request.getInitiator())) {
            throw new IllegalArgumentException("initiator is not on attacker hex");
        }
        if (!request.getInitiator().canAttack()) throw new IllegalArgumentException("initiator cannot attack");
        if (map.getHex(request.getDefenderCoordinate()) == null) {
            throw new IllegalArgumentException("defender coordinate is outside the map");
        }
    }

    private int distance(CombatRequest request) {
        return attackerCoordinate(request).distanceTo(request.getDefenderCoordinate());
    }

    private HexCoordinate attackerCoordinate(CombatRequest request) {
        return request.getAttackingBear() == null
                ? request.getAttackerHex().getCoordinate() : request.getAttackingBear().getPosition();
    }

    private void spendAttackAp(List<MilitaryUnit> participants) {
        for (MilitaryUnit unit : participants) unit.spendAttackAP();
    }

    private void cleanupDeadUnits(CombatRequest request) {
        player.removeDeadUnits();
        if (request != null && request.getTribe() != null) request.getTribe().removeDeadUnits();
    }

    private CombatReport report(CombatRequest request, List<MilitaryUnit> participants,
                                DiceBattle battle, String casualty, int structureDamage,
                                boolean captured) {
        return new CombatReport(attackerLabel(participants), defenderLabel(request),
                battle.attack.getRolls(), battle.defense.getRolls(), battle.result.getAttackerWins(),
                battle.result.getDefenderWins(), casualty, structureDamage, participants.size(),
                request.getTargetType(), battle.wallActive, captured);
    }

    private CombatReport directReport(CombatRequest request, List<MilitaryUnit> participants,
                                      String casualty, int structureDamage, boolean captured) {
        return new CombatReport(attackerLabel(participants), defenderLabel(request),
                Collections.emptyList(), Collections.emptyList(), 0, 0, casualty,
                structureDamage, participants.size(), request.getTargetType(), false, captured);
    }

    private String attackerLabel(List<MilitaryUnit> participants) {
        Set<MilitaryUnitType> types = EnumSet.noneOf(MilitaryUnitType.class);
        for (MilitaryUnit unit : participants) types.add(unit.getMilitaryUnitType());
        return types.toString();
    }

    private String defenderLabel(CombatRequest request) {
        return switch (request.getTargetType()) {
            case MILITARY_UNITS -> "MILITARY";
            case TRIBE_GUARDS -> request.getTribe().getName() + " GUARDS";
            case WILD_ANIMAL -> "BEAR";
            case WILD_ANIMAL_ATTACK -> request.getCivilian().getUnitType().name();
            case CIVILIAN -> request.getCivilian().getUnitType().name();
            case BUILDING -> request.getBuilding().getType().name();
            case WALL -> "WALL";
            case TRIBE_CAMP -> request.getTribe().getName() + " CAMP";
            case EMPTY_HEX -> "EMPTY HEX";
        };
    }

    private static final class DiceBattle {
        private final DiceResult attack;
        private final DiceResult defense;
        private final CombatResult result;
        private final boolean wallActive;
        private DiceBattle(DiceResult attack, DiceResult defense, CombatResult result, boolean wallActive) {
            this.attack = attack;
            this.defense = defense;
            this.result = result;
            this.wallActive = wallActive;
        }
    }
}
