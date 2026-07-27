package model;

public class Builder extends Unit {
    private int charges;

    public Builder(HexCoordinate pos) {
        super(pos, Constants.UnitType.BUILDER);
        this.charges = Constants.BUILDER_CHARGES;
    }

    public int getCharges() { return charges; }
    public boolean hasCharges() { return charges > 0; }

    public void useCharge() {
        if (charges > 0) charges--;
    }
}
