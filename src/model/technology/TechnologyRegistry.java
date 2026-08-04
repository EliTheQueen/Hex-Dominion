package model.technology;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TechnologyRegistry {

    private final Set<TechnologyType> completed =
            new HashSet<>();

    private final Set<TechnologyType> queued =
            new HashSet<>();

    public boolean canQueue(
            TechnologyType technology
    ) {
        requireTechnology(technology);

        return !completed.contains(technology)
                && !queued.contains(technology);
    }

    public void markQueued(
            TechnologyType technology
    ) {
        requireTechnology(technology);

        if (!canQueue(technology)) {
            throw new IllegalStateException(
                    "technology cannot be queued: "
                            + technology
            );
        }

        queued.add(technology);
    }

    public void unqueue(
            TechnologyType technology
    ) {
        requireTechnology(technology);
        queued.remove(technology);
    }

    public void complete(
            TechnologyType technology
    ) {
        requireTechnology(technology);

        if (!queued.remove(technology)) {
            throw new IllegalStateException(
                    "technology is not queued: "
                            + technology
            );
        }

        completed.add(technology);
    }

    public boolean has(
            TechnologyType technology
    ) {
        requireTechnology(technology);
        return completed.contains(technology);
    }

    public boolean isQueued(
            TechnologyType technology
    ) {
        requireTechnology(technology);
        return queued.contains(technology);
    }

    public Set<TechnologyType>
    getCompletedTechnologies() {
        return Collections.unmodifiableSet(
                new HashSet<>(completed)
        );
    }

    private void requireTechnology(
            TechnologyType technology
    ) {
        if (technology == null) {
            throw new IllegalArgumentException(
                    "technology must not be null"
            );
        }
    }
}