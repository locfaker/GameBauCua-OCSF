package game;

import models.DiceSymbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiceEngine {
    private Random random = new Random();

    public List<DiceSymbol> roll() {
        List<DiceSymbol> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            result.add(DiceSymbol.fromValue(random.nextInt(6)));
        }
        return result;
    }
}
