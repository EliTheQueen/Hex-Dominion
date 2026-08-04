package model.tribe;

public class TribeWarService {

    public DiplomacyResult declareWar(Tribe tribe) {
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

        tribe.getRelation().becomeEnemy();

        return DiplomacyResult.SUCCESS;
    }
}