package model;

import java.util.ArrayList;

public class GameState {

    protected ArrayList<Unit> units;
    protected ArrayList<Hex>  hexes;

    public enum UnitState {
        BUSY,
        FREE
    }

    public enum TerrainType {
        PLAIN, FOREST, MOUNTAIN, GRASSLAND;
    }

    public enum NaturalResourceType {
        NONE, TREE, STONE, IRON, WHEAT, RICE, COW, SHEEP;
    }

    public enum ResourceType {
        WOOD, FOOD, STONE, IRON;
    }

    public void addHexes(Hex hex) {
        this.hexes.add(hex);
    }
}
