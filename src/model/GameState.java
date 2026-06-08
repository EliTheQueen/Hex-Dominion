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

    }

    public enum NaturalResourceType {
        NONE,

    }

    public void addHexes(Hex hex) {
        this.hexes.add(hex);
    }
}
