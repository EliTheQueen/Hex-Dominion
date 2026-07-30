package model.combat;

import java.util.ArrayList;
import java.util.List;

public class DiceResult {
    private List<Integer> diceRollerResults = new ArrayList<>();

    public DiceResult(List diceRollerResults) {
        if (diceRollerResults == null)
            throw new NullPointerException("diceRollerResults is null");
        this.diceRollerResults = diceRollerResults;
    }

    public List<Integer> getRolls() {
        return diceRollerResults;
    }

    public int size() {
        return diceRollerResults.size();
    }
}
