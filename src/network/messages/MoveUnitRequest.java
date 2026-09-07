package network.messages;

import network.Request;

public final class MoveUnitRequest extends Request {
    private final Integer unitIndex;
    private final Integer x;
    private final Integer y;

    public MoveUnitRequest(int unitIndex, int x, int y) {
        this.unitIndex = unitIndex;
        this.x = x;
        this.y = y;
    }

    public Integer getUnitIndex() { return unitIndex; }
    public Integer getX() { return x; }
    public Integer getY() { return y; }
}
