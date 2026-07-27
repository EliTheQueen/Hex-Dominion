package model.technology;

public class SailingEffect implements TechnologyEffect {

    @Override
    public TechnologyType getTechnologyType() {
        return TechnologyType.SAILING;
    }

    @Override
    public void apply(TechnologyEffectTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }

        target.enableSailing();
    }

}
