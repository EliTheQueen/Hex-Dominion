package network.messages;

import network.Request;

public final class BuildRequest extends Request {
    private final Integer builderIndex;
    private final String buildingType;
    private final Integer x;
    private final Integer y;

    public BuildRequest(int builderIndex, String buildingType, int x, int y) {
        this.builderIndex = builderIndex;
        this.buildingType = buildingType;
        this.x = x;
        this.y = y;
    }

    public Integer getBuilderIndex() { return builderIndex; }
    public String getBuildingType() { return buildingType; }
    public Integer getX() { return x; }
    public Integer getY() { return y; }
}
