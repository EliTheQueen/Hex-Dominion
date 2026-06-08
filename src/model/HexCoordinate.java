package model;

public class HexCoordinate {

    private static final int[][] DIRECTIONS = {
            {1, 0}, {1, -1}, {0, -1},
            {-1, 0}, {-1, 1}, {0, 1}
    };
    private final int q;
    private final int r;

    public HexCoordinate(int q, int r) {
        this.q = q;
        this.r = r;
    }

    public int getQ() { return q; }
    public int getR() { return r; }

}
