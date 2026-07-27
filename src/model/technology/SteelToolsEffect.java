package model.technology;

public class SteelToolsEffect implements TechnologyEffect{
    @Override
    public TechnologyType getTechnologyType() {
        return TechnologyType.STEEL_TOOLS;
    }

    @Override
    public void apply(TechnologyEffectTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }

        target.enableSteelTools();
    }
}
