package dev.ked.quetzalmap.manager;

import dev.ked.quetzalmap.QuetzalMapPlugin;
import dev.ked.quetzalmap.model.Marker;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkerManager {

    private final QuetzalMapPlugin plugin;
    private final Map<String, Marker> markers;

    public MarkerManager(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
        this.markers = new HashMap<>();
    }

    /**
     * Register a marker
     */
    public void addMarker(Marker marker) {
        markers.put(marker.getId(), marker);
    }

    /**
     * Remove a marker by ID
     */
    public void removeMarker(String markerId) {
        markers.remove(markerId);
    }

    /**
     * Update a marker's location
     */
    public void updateMarker(String markerId, Location newLocation) {
        Marker marker = markers.get(markerId);
        if (marker != null) {
            marker.setLocation(newLocation);
        }
    }

    /**
     * Get all markers
     */
    public List<Marker> getAllMarkers() {
        return new ArrayList<>(markers.values());
    }

    /**
     * Get markers by type
     */
    public List<Marker> getMarkersByType(String type) {
        List<Marker> result = new ArrayList<>();
        for (Marker marker : markers.values()) {
            if (marker.getType().equals(type)) {
                result.add(marker);
            }
        }
        return result;
    }

    /**
     * Clear all markers
     */
    public void clearMarkers() {
        markers.clear();
    }

    /**
     * Clear markers of a specific type
     */
    public void clearMarkersByType(String type) {
        markers.entrySet().removeIf(entry -> entry.getValue().getType().equals(type));
    }

    /**
     * Get marker by ID
     */
    public Marker getMarker(String markerId) {
        return markers.get(markerId);
    }

    /**
     * Get marker count
     */
    public int getMarkerCount() {
        return markers.size();
    }

    /**
     * Get marker count by type
     */
    public int getMarkerCountByType(String type) {
        return (int) markers.values().stream()
            .filter(m -> m.getType().equals(type))
            .count();
    }
}
