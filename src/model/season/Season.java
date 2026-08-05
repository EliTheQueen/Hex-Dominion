package model.season;

public enum Season {

    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    public Season next() {
        Season[] seasons = values();

        int nextIndex = (ordinal() + 1) % seasons.length;

        return seasons[nextIndex];
    }
}