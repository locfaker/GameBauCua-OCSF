package models;

public enum DiceSymbol {
    BAU(0, "Bầu"),
    CUA(1, "Cua"),
    TOM(2, "Tôm"),
    CA(3, "Cá"),
    GA(4, "Gà"),
    NAI(5, "Nai");

    private final int value;
    private final String name;

    DiceSymbol(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() { return value; }
    public String getName() { return name; }

    public static DiceSymbol fromValue(int value) {
        for (DiceSymbol s : values()) {
            if (s.value == value) return s;
        }
        return BAU;
    }
}
