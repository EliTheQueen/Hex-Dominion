package model.combat;

import java.util.ArrayList;
import java.util.List;

public class DiceRoller {

    public DiceRoller() {
    }

    public DiceResult rollD6(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be a positive integer");
        }

        List<Integer> diceRollerResults = new ArrayList<Integer>();
        for (int i = 0; i < count; i++) {
            int randomNumber = (int) (Math.random() * 6) + 1;
            diceRollerResults.add(randomNumber);
        }
        return new DiceResult(diceRollerResults);
    }
}
