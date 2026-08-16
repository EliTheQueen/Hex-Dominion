package model.disaster;

import model.season.Season;

//دو سؤال اصلی ابتدای Turn:
//آیا Disaster رخ دهد؟
//کدام Disaster انتخاب شود؟
public final class DisasterEligibility {

    private DisasterEligibility() {
    }

    public static boolean isEligible(DisasterType disasterType, Season season, boolean navalSystemEnabled) {
        if (disasterType == null || season == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }

        switch (disasterType) {
            case FLOOD:
                return season == Season.AUTUMN;

            case SEA_STORM:
                return true;

            case EARTHQUAKE:
            case BEAR_ATTACK:
            case TSUNAMI:
            case VOLCANIC_ERUPTION:
            case TORNADO:
            case AVALANCHE:
                return true;

            default:
                throw new IllegalStateException("Unsupported disaster type: " + disasterType
                );
        }
    }
}