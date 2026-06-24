package model;

public class ResourceAmount {
    private int food;
    private int wood;
    private int stone;
    private int iron;

    public ResourceAmount(int food, int wood, int stone, int iron) {
        this.food = food;
        this.wood = wood;
        this.stone = stone;
        this.iron = iron;
    }

    public static ResourceAmount of(int i, int i1, int i2, int i3) {
        
    }

    public static ResourceAmount zero() {
    }

    public int getFood() { return food; }
    public int getWood() { return wood; }
    public int getStone() { return stone; }
    public int getIron() { return iron; }

    public void addResourceAmount(ResourceAmount amount) {
        food += amount.getFood();
        wood += amount.getWood();
        stone += amount.getStone();
        iron += amount.getIron();
    }

    public void subtractResourceAmount(ResourceAmount amount) {
        food -= amount.getFood();
        wood -= amount.getWood();
        stone -= amount.getStone();
        iron -= amount.getIron();
    }

    public boolean hasEnoughResource(ResourceAmount cost) {
        return food >= cost.getFood() && wood >= cost.getWood() && stone >= cost.getStone() && iron >= cost.getIron();
    }
}
