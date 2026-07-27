package model.technology;

import model.ResourceAmount;
import model.townhall.AbstractProductionCommand;

public class ResearchTechnologyCommand extends AbstractProductionCommand {
    private final TechnologyRegistry technologyRegistry;
    private final TechnologyEffectService technologyEffectService;
    private final TechnologyEffectTarget technologyEffectTarget;
    private final TechnologyType technologyType;


    public ResearchTechnologyCommand(TechnologyRegistry technologyRegistry,
                                     TechnologyEffectService technologyEffectService,
                                     TechnologyEffectTarget technologyEffectTarget,
                                     TechnologyType technologyType,
                                     ResourceAmount cost,
                                     int totalTurns)
    {
        super(cost, totalTurns);
        if (technologyRegistry == null) {
            throw new IllegalArgumentException("technologyRegistry cannot be null");
        }
        if (technologyEffectService == null) {
            throw new IllegalArgumentException("technologyEffectService cannot be null");
        }
        if (technologyEffectTarget == null) {
            throw new IllegalArgumentException("technologyEffectTarget cannot be null");
        }
        if (technologyType == null) {
            throw new IllegalArgumentException("technologyType cannot be null");
        }

        this.technologyRegistry = technologyRegistry;
        this.technologyEffectService = technologyEffectService;
        this.technologyEffectTarget = technologyEffectTarget;
        this.technologyType = technologyType;

    }

    public TechnologyType getTechnologyType() {
        return technologyType;
    }

    @Override
    protected void executeEffect() {
        technologyRegistry.complete(technologyType);
        technologyEffectService.apply(technologyEffectTarget, technologyType);
    }

    @Override
    public void onStarted() {
        technologyRegistry.markQueued(technologyType);
    }
}
