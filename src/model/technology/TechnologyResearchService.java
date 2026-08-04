package model.technology;

import model.townhall.CommandStartResult;
import model.townhall.TownHall;
import model.townhall.TownHallCommandService;

public class TechnologyResearchService {

    private final TownHall townHall;
    private final TechnologyRegistry registry;

    private final TechnologyEffectService effectService;

    private final TechnologyEffectTarget effectTarget;

    private final TownHallCommandService commandService;

    public TechnologyResearchService(
            TownHall townHall,
            TechnologyRegistry registry,
            TechnologyEffectService effectService,
            TechnologyEffectTarget effectTarget,
            TownHallCommandService commandService
    ) {
        if (townHall == null
                || registry == null
                || effectService == null
                || effectTarget == null
                || commandService == null) {
            throw new IllegalArgumentException(
                    "dependencies must not be null"
            );
        }

        this.townHall = townHall;
        this.registry = registry;
        this.effectService = effectService;
        this.effectTarget = effectTarget;
        this.commandService = commandService;
    }

    public ResearchStartResult startResearch(
            TechnologyType technology
    ) {
        if (technology == null) {
            return ResearchStartResult.INVALID_REQUEST;
        }

        if (!technology.isUnlockedAt(
                townHall.getLevel()
        )) {
            return ResearchStartResult.LEVEL_TOO_LOW;
        }

        if (!registry.canQueue(technology)) {
            return ResearchStartResult
                    .ALREADY_RESEARCHED_OR_QUEUED;
        }

        ResearchTechnologyCommand command =
                new ResearchTechnologyCommand(
                        registry,
                        effectService,
                        effectTarget,
                        technology,
                        technology.getCost(),
                        technology.getResearchTurns()
                );

        CommandStartResult result =
                commandService.startCommand(command);

        switch (result) {
            case STARTED:
                return ResearchStartResult.STARTED;

            case TOWN_HALL_BUSY:
                return ResearchStartResult
                        .TOWN_HALL_BUSY;

            case INSUFFICIENT_RESOURCES:
                return ResearchStartResult
                        .INSUFFICIENT_RESOURCES;

            default:
                return ResearchStartResult
                        .INVALID_REQUEST;
        }
    }
}