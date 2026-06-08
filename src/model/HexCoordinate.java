package model;

import java.util.ArrayList;

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

    public ArrayList<HexCoordinate> findNeighbours() {
        ArrayList<HexCoordinate> neighbours = new ArrayList<>();

        for (int[] direction : DIRECTIONS) {
            int newQ = this.q + direction[0];
            int newR = this.r + direction[1];

            neighbours.add(new HexCoordinate(newQ, newR));
        }
        return neighbours;
    }

    public int distanceTo(HexCoordinate other) {
        int dq = this.q - other.q;
        int dr = this.r - other.r;
        int ds = (- this.q - this.r) - (- other.q - other.r);

        return (Math.abs(dq) + Math.abs(dr) + Math.abs(ds)) / 2;
    }

    public int getQ() { return q; }
    public int getR() { return r; }

}
