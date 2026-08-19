package model.combat;

import java.util.ArrayList;
import java.util.List;

public class DiceResult {
    private List<Integer> diceRollerResults = new ArrayList<>();

    public DiceResult(List<Integer> diceRollerResults) {
        if (diceRollerResults == null)
            throw new NullPointerException("diceRollerResults is null");
        this.diceRollerResults = new ArrayList<>(diceRollerResults);
    }

    public List<Integer> getRolls() {
        return diceRollerResults;
    }

    public int size() {
        return diceRollerResults.size();
    }
}
