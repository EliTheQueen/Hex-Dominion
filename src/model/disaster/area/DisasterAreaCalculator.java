package model.disaster.area;

import model.GameMap;
import model.HexCoordinate;

public interface DisasterAreaCalculator extends java.io.Serializable {

    DisasterArea calculate(HexCoordinate origin, GameMap map);
}
