package network.massages;

import network.Request;

public final class MoveUnitRequest extends Request {
    private final int unitIndex;
    private final int x;
    private final int y;

    public MoveUnitRequest(int unitIndex, int x, int y) {
        this.unitIndex = unitIndex;
        this.x = x;
        this.y = y;
    }

    public int getUnitIndex() { return unitIndex; }
    public int getX() { return x; }
    public int getY() { return y; }
}
