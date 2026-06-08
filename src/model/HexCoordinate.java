package model;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

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

    public List<HexCoordinate> findNeighbours() {
        List<HexCoordinate> neighbours = new ArrayList<>();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HexCoordinate)) return false;
        HexCoordinate that = (HexCoordinate) o;
        return this.q == that.q && this.r == that.r;
    }

    @Override
    public int hashCode() {
        return Objects.hash(q, r);
    }

    public int getQ() { return q; }
    public int getR() { return r; }

}
