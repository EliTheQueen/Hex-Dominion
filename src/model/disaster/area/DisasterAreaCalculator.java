package model.disaster.area;

import model.GameMap;
import model.HexCoordinate;

public interface DisasterAreaCalculator {

    DisasterArea calculate(HexCoordinate origin, GameMap map);
}
