package model.save;

public enum SaveSlot {
    MANUAL_1("Manual Slot 1", "manual-1.hds"),
    MANUAL_2("Manual Slot 2", "manual-2.hds"),
    MANUAL_3("Manual Slot 3", "manual-3.hds"),
    AUTOSAVE("Autosave", "autosave.hds");

    private final String displayName;
    private final String fileName;
    SaveSlot(String displayName, String fileName) {
        this.displayName = displayName;
        this.fileName = fileName;
    }
    public String getDisplayName() { return displayName; }
    public String getFileName() { return fileName; }
    public boolean isManual() { return this != AUTOSAVE; }
}
