package dev.ked.quetzalmap.model;

public enum MarkerType {
    STORM("storms"),
    SHOP("shops"),
    TRANSPORTER("transporters"),
    TRADE_POST("trade-posts"),
    EVENT("events"),
    DUNGEON("dungeons"),
    TOWN("towns"),
    NATION("nations"),
    CUSTOM("custom");

    private final String configKey;

    MarkerType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static MarkerType fromConfigKey(String key) {
        for (MarkerType type : values()) {
            if (type.configKey.equals(key)) {
                return type;
            }
        }
        return CUSTOM;
    }
}
