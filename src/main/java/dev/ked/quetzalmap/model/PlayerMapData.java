package dev.ked.quetzalmap.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMapData {

    private final UUID playerId;
    private int zoomLevel;
    private final Map<String, Boolean> markerVisibility;

    public PlayerMapData(UUID playerId) {
        this.playerId = playerId;
        this.zoomLevel = 1024; // Default zoom
        this.markerVisibility = new HashMap<>();

        // Initialize all marker types as visible by default
        for (MarkerType type : MarkerType.values()) {
            markerVisibility.put(type.getConfigKey(), true);
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public boolean isMarkerVisible(String markerType) {
        return markerVisibility.getOrDefault(markerType, true);
    }

    public void setMarkerVisible(String markerType, boolean visible) {
        markerVisibility.put(markerType, visible);
    }

    public void toggleMarker(String markerType) {
        boolean current = isMarkerVisible(markerType);
        setMarkerVisible(markerType, !current);
    }

    public Map<String, Boolean> getMarkerVisibility() {
        return new HashMap<>(markerVisibility);
    }
}
