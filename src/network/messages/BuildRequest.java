package network.messages;

import network.Request;

public final class BuildRequest extends Request {
    private final int builderIndex;
    private final String buildingType;
    private final int x;
    private final int y;

    public BuildRequest(int builderIndex, String buildingType, int x, int y) {
        this.builderIndex = builderIndex;
        this.buildingType = buildingType;
        this.x = x;
        this.y = y;
    }

    public int getBuilderIndex() { return builderIndex; }
    public String getBuildingType() { return buildingType; }
    public int getX() { return x; }
    public int getY() { return y; }
}
