package model;

public class ResourceStorage {
    private int currentFood;
    private int currentWood;
    private int currentStone;
    private int currentIron;

    private int capacityFood;
    private int capacityWood;
    private int capacityStone;
    private int capacityIron;

    public ResourceStorage(int capacityFood, int capacityWood, int capacityStone, int capacityIron) {
        this.capacityFood = capacityFood;
        this.capacityWood = capacityWood;
        this.capacityStone = capacityStone;
        this.capacityIron = capacityIron;

        this.currentFood = 0;
        this.currentWood = 0;
        this.currentStone = 0;
        this.currentIron = 0;
    }

    public void addResource(ResourceAmount amount) {
        currentFood = Math.min(currentFood + amount.getFood(), capacityFood);
        currentWood = Math.min(currentWood + amount.getWood(), capacityWood);
        currentStone = Math.min(currentStone + amount.getStone(), capacityStone);
        currentIron = Math.min(currentIron + amount.getIron(), capacityIron);
    }

    public boolean spendResource(ResourceAmount cost) {
        ResourceAmount current = new ResourceAmount(
                currentFood,
                currentWood,
                currentStone,
                currentIron
        );

        if (!current.hasEnoughResource(cost)) {
            return false;
        }

        currentFood -= cost.getFood();
        currentWood -= cost.getWood();
        currentStone -= cost.getStone();
        currentIron -= cost.getIron();

        return true;
    }

    public int forceSpendFood(int amount) {
        int shortage = amount - currentFood;

        currentFood -= amount;

        if (currentFood < 0) {
            currentFood = 0;
        }

        if (shortage > 0) {
            return shortage;
        }

        return 0;
    }

    public void upgradeCapacity(ResourceAmount extraCapacity) {
        capacityFood += extraCapacity.getFood();
        capacityWood += extraCapacity.getWood();
        capacityStone += extraCapacity.getStone();
        capacityIron += extraCapacity.getIron();
    }

    public String getResourceStatus() {
        return "Food: " + currentFood + "/" + capacityFood
                + ", Wood: " + currentWood + "/" + capacityWood
                + ", Stone: " + currentStone + "/" + capacityStone
                + ", Iron: " + currentIron + "/" + capacityIron;
    }
}
