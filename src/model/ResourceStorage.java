package model;
import static model.Constants.ResourceType;

public class ResourceStorage {
    private ResourceAmount current;
    private ResourceAmount capacity;

    public ResourceStorage(int cap) {
        current = ResourceAmount.of(0, 0, 0, 0);
        capacity = ResourceAmount.of(cap, cap, cap, cap);
    }

    public void addResources(ResourceAmount amount) {
        for (ResourceType r : ResourceType.values()) {
            int newVal = Math.min(current.get(r) + amount.get(r), capacity.get(r));
            current.set(r, newVal);
        }
    }

    public boolean canAfford(ResourceAmount cost) {
        return current.hasEnough(cost);
    }

    public boolean spend(ResourceAmount cost) {
        if (!canAfford(cost))
            return false;
        for (ResourceType r : ResourceType.values()) {
            current.set(r, current.get(r) - cost.get(r));
        }
        return true;
    }

    //in ro dobare bekhoon base ghahti!
    public int forceSpendFood(int amount) {
        int have = current.get(ResourceType.FOOD);
        int shortage = Math.max(0, amount - have);
        current.set(ResourceType.FOOD, Math.max(0, have - amount));
        return shortage;
    }

    public void upgradeCapacity(ResourceAmount extra) {
        for (ResourceType r : ResourceType.values()) {
            capacity.set(r, capacity.get(r) + extra.get(r));
        }
    }

    public ResourceAmount getCurrent() { return current.copy(); }
    public ResourceAmount getCapacity() { return capacity.copy(); }

    public int get(ResourceType r) { return current.get(r); }
    public int getCap(ResourceType r) { return capacity.get(r); }

    public String getStatus() {
        return "Food:" + current.get(ResourceType.FOOD) + "/" + capacity.get(ResourceType.FOOD)
             + " Wood:" + current.get(ResourceType.WOOD) + "/" + capacity.get(ResourceType.WOOD)
             + " Stone:" + current.get(ResourceType.STONE) + "/" + capacity.get(ResourceType.STONE)
             + " Iron:" + current.get(ResourceType.IRON) + "/" + capacity.get(ResourceType.IRON);
    }
}
