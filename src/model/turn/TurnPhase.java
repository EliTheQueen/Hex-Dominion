package model.turn;

/** The one authoritative order for a complete end-turn transaction. */
public enum TurnPhase {
    BEGINNING_OF_TURN,
    DOMAIN_AND_COMMANDS,
    PRODUCTION_AND_UPKEEP,
    POPULATION_AND_UNITS,
    CALENDAR_AND_TRIBES,
    ACTIVE_EVENTS,
    END_CONDITIONS
}
