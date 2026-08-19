package model.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiceRoller implements java.io.Serializable {
    private final Random random;

    public DiceRoller() {
        this(new Random());
    }

    public DiceRoller(Random random) {
        if (random == null) throw new IllegalArgumentException("random must not be null");
        this.random = random;
    }

    public DiceResult rollD6(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be a positive integer");
        }

        List<Integer> diceRollerResults = new ArrayList<Integer>();
        for (int i = 0; i < count; i++) {
            int randomNumber = random.nextInt(6) + 1;
            diceRollerResults.add(randomNumber);
        }
        return new DiceResult(diceRollerResults);
    }
}
