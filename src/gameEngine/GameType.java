package gameEngine;

public enum GameType {
    TOTALE_PLUS("TOTALE+", "Total Plus"),
    TOTALE_MINUS("TOTALE-", "Total Minus"),
    DIAMONDS("DIAMONDS", "Diamonds"),
    QUINS("QUINS", "Quins"),
    HEARTS_KING("HEARTS_KING", "Hearts King"),
    LEVATE("LEVATE", "Levate"),
    WHIST("WHIST", "Whist");

    private final String code;
    private final String displayName;

    GameType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
}
