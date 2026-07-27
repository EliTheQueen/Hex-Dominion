package model.technology;

import java.util.HashSet;
import java.util.Set;

public class TechnologyRegistry {
    //قط وضعیت تحقیق‌های یک Empire را نگه می‌دارد.
    private final Set<TechnologyType> completed;
    private final Set<TechnologyType> queued;

    public  TechnologyRegistry() {
        this.completed = new  HashSet<>();
        this.queued = new  HashSet<>();
    }

    public boolean canQueue(TechnologyType technology) {
        if (technology == null) {
            throw new IllegalArgumentException("Technology object cannot be null");
        }
        if (completed.contains(technology)) {
            return false;
        }
        return !queued.contains(technology);
    }

    public void markQueued(TechnologyType technology) {
        if (technology == null) {
            throw new IllegalArgumentException("Technology object cannot be null");
        }
        if (canQueue(technology)) {
            queued.add(technology);
            return;
        }
        throw new IllegalArgumentException("Technology object cannot be queued");
    }

    public void complete(TechnologyType technology) {
        if (technology == null) {
            throw new IllegalArgumentException("Technology object cannot be null");
        }
        if (completed.contains(technology) || !queued.contains(technology)) {
            throw new IllegalArgumentException("Technology already completed or already queued.");
        }
        queued.remove(technology);
        completed.add(technology);
    }

    public boolean has(TechnologyType technology) {
        if (technology == null) {
            throw new IllegalArgumentException("Technology object cannot be null");
        }
        return completed.contains(technology);
    }

}
