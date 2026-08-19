package model.save;

import java.time.Instant;

public final class SavePreview {
    private final SaveSlot slot;
    private final boolean present;
    private final boolean corrupted;
    private final String name;
    private final int turn;
    private final String season;
    private final int townHallLevel;
    private final Instant savedAt;

    public SavePreview(SaveSlot slot, boolean present, boolean corrupted, String name,
                       int turn, String season, int townHallLevel, Instant savedAt) {
        this.slot = slot; this.present = present; this.corrupted = corrupted; this.name = name;
        this.turn = turn; this.season = season; this.townHallLevel = townHallLevel; this.savedAt = savedAt;
    }
    public SaveSlot getSlot() { return slot; }
    public boolean isPresent() { return present; }
    public boolean isCorrupted() { return corrupted; }
    public String getName() { return name; }
    public int getTurn() { return turn; }
    public String getSeason() { return season; }
    public int getTownHallLevel() { return townHallLevel; }
    public Instant getSavedAt() { return savedAt; }
}
