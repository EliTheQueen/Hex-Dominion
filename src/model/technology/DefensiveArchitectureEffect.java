package model.technology;

public class DefensiveArchitectureEffect implements TechnologyEffect{
    @Override
    public TechnologyType getTechnologyType() {
        return TechnologyType.DEFENSIVE_ARCHITECTURE;
    }

    @Override
    public void apply(TechnologyEffectTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        target.enableDefensiveArchitecture();
    }
}
