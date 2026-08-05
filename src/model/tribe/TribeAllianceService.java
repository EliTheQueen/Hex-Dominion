package model.tribe;

//این Service تصمیم می‌گیرد اتحاد شکل بگیرد یا نه.
public class TribeAllianceService {

    private static final int REQUIRED_RELATION = 70;

    private final TribeAllianceRegistry allianceRegistry;

    public TribeAllianceService(TribeAllianceRegistry allianceRegistry) {
        if (allianceRegistry == null) {
            throw new IllegalArgumentException("allianceRegistry must not be null");
        }

        this.allianceRegistry = allianceRegistry;
    }

    public DiplomacyResult requestAlliance(Tribe tribe) {
        if (tribe == null) {
            return DiplomacyResult.INVALID_REQUEST;
        }

        if (!tribe.isDiscovered()) {
            return DiplomacyResult.TRIBE_NOT_DISCOVERED;
        }

        if (tribe.isDefeated()) {
            return DiplomacyResult.TRIBE_DEFEATED;
        }

        if (tribe.getRelation().isEnemy()) {
            return DiplomacyResult.ALREADY_ENEMY;
        }

        if (tribe.getRelation().getScore() < REQUIRED_RELATION) {
            return DiplomacyResult.RELATION_TOO_LOW;
        }

        if (!allianceRegistry.canAllyWith(tribe)) {
            return DiplomacyResult.ALLIANCE_RESTRICTED;
        }

        allianceRegistry.addAlliance(tribe);

        return DiplomacyResult.SUCCESS;
    }

    public void refreshAllianceState(Tribe tribe) {
        if (tribe == null) {
            return;
        }

        if (tribe.getRelation().getScore() < REQUIRED_RELATION) {
            allianceRegistry.removeAlliance(tribe);
        }
    }

    public void breakAlliance(Tribe tribe) {
        if (tribe == null) {
            return;
        }

        allianceRegistry.removeAlliance(tribe);
    }
}