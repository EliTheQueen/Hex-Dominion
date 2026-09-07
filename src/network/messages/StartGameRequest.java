package network.messages;

import network.Request;

public final class StartGameRequest extends Request {
    private final Integer width;
    private final Integer height;
    private final Long seed;

    public StartGameRequest(Integer width, Integer height, Long seed) {
        this.width = width;
        this.height = height;
        this.seed = seed;
    }

    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public Long getSeed() { return seed; }
}
