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

    public int getFood() { return food; }
    public int getWood() { return wood; }
    public int getStone() { return stone; }
    public int getIron() { return iron; }
}
