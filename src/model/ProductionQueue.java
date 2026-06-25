package model;

import java.util.ArrayList;
import java.util.List;

/** The Town Hall's sequential production queue. Only the front task progresses each turn. */
public class ProductionQueue {
    private final List<ProductionTask> tasks = new ArrayList<>();

    public void enqueue(ProductionTask task) { tasks.add(task); }

    public boolean isEmpty() { return tasks.isEmpty(); }
    public int size() { return tasks.size(); }
    public List<ProductionTask> getTasks() { return tasks; }

    public ProductionTask getFront() { return tasks.isEmpty() ? null : tasks.get(0); }

    public void cancelFront() { if (!tasks.isEmpty()) tasks.remove(0); }

    /**
     * Advances the front task by one turn. If it finishes it is removed and returned so the
     * caller can apply its effect (spawn the unit / unlock the tech); otherwise returns null.
     */
    public ProductionTask advance() {
        if (tasks.isEmpty()) return null;
        ProductionTask front = tasks.get(0);
        front.tick();
        if (front.isComplete()) {
            tasks.remove(0);
            return front;
        }
        return null;
    }
}
