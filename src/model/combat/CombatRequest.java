package model.combat;

import model.Building;
import model.HexCoordinate;
import model.Unit;
import model.Wall;
import model.disaster.Bear;
import model.military.MilitaryHex;
import model.military.MilitaryUnit;
import model.tribe.Tribe;

/**
 * Immutable hex-based input for every combat path.  Target-specific factories
 * make invalid combinations difficult to construct while keeping the resolver
 * independent from Swing/controller state.
 */
public final class CombatRequest {
    private final MilitaryUnit initiator;
    private final MilitaryHex attackerHex;
    private final HexCoordinate defenderCoordinate;
    private final CombatTargetType targetType;
    private final MilitaryHex defenderMilitaryHex;
    private final Building building;
    private final Wall wallTarget;
    private final Tribe tribe;
    private final Bear bear;
    private final Bear attackingBear;
    private final Unit civilian;

    private CombatRequest(MilitaryUnit initiator, MilitaryHex attackerHex,
                          HexCoordinate defenderCoordinate, CombatTargetType targetType,
                          MilitaryHex defenderMilitaryHex, Building building, Wall wallTarget,
                          Tribe tribe, Bear bear, Bear attackingBear, Unit civilian) {
        if (defenderCoordinate == null || targetType == null) {
            throw new IllegalArgumentException("combat request fields must not be null");
        }
        if (attackingBear == null && (initiator == null || attackerHex == null)) {
            throw new IllegalArgumentException("combat request needs a military or bear attacker");
        }
        this.initiator = initiator;
        this.attackerHex = attackerHex;
        this.defenderCoordinate = defenderCoordinate;
        this.targetType = targetType;
        this.defenderMilitaryHex = defenderMilitaryHex;
        this.building = building;
        this.wallTarget = wallTarget;
        this.tribe = tribe;
        this.bear = bear;
        this.attackingBear = attackingBear;
        this.civilian = civilian;
    }

    public static CombatRequest military(MilitaryUnit initiator, MilitaryHex attacker,
                                         MilitaryHex defender) {
        return new CombatRequest(initiator, attacker, defender.getCoordinate(),
                CombatTargetType.MILITARY_UNITS, defender, null, null, null, null, null, null);
    }

    public static CombatRequest tribeGuards(MilitaryUnit initiator, MilitaryHex attacker, Tribe tribe) {
        return new CombatRequest(initiator, attacker, tribe.getCampCoordinate(),
                CombatTargetType.TRIBE_GUARDS, null, null, null, tribe, null, null, null);
    }

    public static CombatRequest wildAnimal(MilitaryUnit initiator, MilitaryHex attacker, Bear bear) {
        return new CombatRequest(initiator, attacker, bear.getPosition(),
                CombatTargetType.WILD_ANIMAL, null, null, null, null, bear, null, null);
    }

    public static CombatRequest civilian(MilitaryUnit initiator, MilitaryHex attacker, Unit civilian) {
        return new CombatRequest(initiator, attacker, civilian.getPosition(),
                CombatTargetType.CIVILIAN, null, null, null, null, null, null, civilian);
    }

    public static CombatRequest building(MilitaryUnit initiator, MilitaryHex attacker,
                                         Building building, MilitaryHex defenders) {
        return new CombatRequest(initiator, attacker, building.getPosition(),
                CombatTargetType.BUILDING, defenders, building, null, null, null, null, null);
    }

    public static CombatRequest wall(MilitaryUnit initiator, MilitaryHex attacker,
                                     HexCoordinate defenderCoordinate, Wall wall,
                                     MilitaryHex defenders) {
        return new CombatRequest(initiator, attacker, defenderCoordinate,
                CombatTargetType.WALL, defenders, null, wall, null, null, null, null);
    }

    public static CombatRequest tribeCamp(MilitaryUnit initiator, MilitaryHex attacker, Tribe tribe) {
        return new CombatRequest(initiator, attacker, tribe.getCampCoordinate(),
                CombatTargetType.TRIBE_CAMP, null, null, null, tribe, null, null, null);
    }

    public static CombatRequest emptyHex(MilitaryUnit initiator, MilitaryHex attacker,
                                         HexCoordinate defenderCoordinate) {
        return new CombatRequest(initiator, attacker, defenderCoordinate,
                CombatTargetType.EMPTY_HEX, null, null, null, null, null, null, null);
    }

    public static CombatRequest bearAttack(Bear bear, Unit target, MilitaryHex militaryDefenders) {
        if (bear == null || target == null) throw new IllegalArgumentException("bear and target are required");
        return new CombatRequest(null, null, target.getPosition(),
                CombatTargetType.WILD_ANIMAL_ATTACK, militaryDefenders, null, null,
                null, null, bear, target);
    }

    public MilitaryUnit getInitiator() { return initiator; }
    public MilitaryHex getAttackerHex() { return attackerHex; }
    public HexCoordinate getDefenderCoordinate() { return defenderCoordinate; }
    public CombatTargetType getTargetType() { return targetType; }
    public MilitaryHex getDefenderMilitaryHex() { return defenderMilitaryHex; }
    public Building getBuilding() { return building; }
    public Wall getWallTarget() { return wallTarget; }
    public Tribe getTribe() { return tribe; }
    public Bear getBear() { return bear; }
    public Bear getAttackingBear() { return attackingBear; }
    public Unit getCivilian() { return civilian; }
}
