package model.technology;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TechnologyEffectService implements java.io.Serializable {
    private final Map<TechnologyType, TechnologyEffect> technologyEffectMap = new HashMap<>();

    public TechnologyEffectService(List<TechnologyEffect>  technologyEffects) {
        if (technologyEffects == null) {
            throw new IllegalArgumentException("technologyEffects must not be null");
        }

        for (TechnologyEffect technologyEffect : technologyEffects) {
            if (technologyEffect == null) {
                throw new IllegalArgumentException("technologyEffect must not be null");
            }
            if (technologyEffectMap.containsKey(technologyEffect.getTechnologyType())) {
                throw new IllegalArgumentException("technologyEffect already exists");
            }

            technologyEffectMap.put(technologyEffect.getTechnologyType(), technologyEffect);
        }
    }

    public TechnologyEffect getTechnologyEffect(TechnologyType technologyType) {
        if (technologyType == null) {
            throw new IllegalArgumentException("technologyType must not be null");
        }
        return technologyEffectMap.get(technologyType);
    }

    public void apply(TechnologyEffectTarget target, TechnologyType technologyType) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        if (technologyType == null) {
            throw new IllegalArgumentException("technologyType must not be null");
        }

        TechnologyEffect effect = getTechnologyEffect(technologyType);

        if (effect == null) {
            throw new IllegalArgumentException("technologyEffect must not be null");
        }

        effect.apply(target);
    }

}
