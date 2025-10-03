package dev.ked.quetzalmap.model;

import org.bukkit.Location;
import org.bukkit.map.MapCursor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Marker {

    private final String id;
    private final String type;
    private Location location;
    private String name;
    private MapCursor.Type cursorType;
    private byte color;
    private Map<String, Object> data;

    public Marker(String id, String type, Location location, String name) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.name = name;
        this.cursorType = MapCursor.Type.RED_X;
        this.color = 0;
        this.data = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MapCursor.Type getCursorType() {
        return cursorType;
    }

    public void setCursorType(MapCursor.Type cursorType) {
        this.cursorType = cursorType;
    }

    public byte getColor() {
        return color;
    }

    public void setColor(byte color) {
        this.color = color;
    }

    public void setData(String key, Object value) {
        this.data.put(key, value);
    }

    public Object getData(String key) {
        return this.data.get(key);
    }

    public <T> T getData(String key, Class<T> type) {
        Object value = this.data.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    public boolean hasData(String key) {
        return this.data.containsKey(key);
    }

    public static String generateId(String type) {
        return type + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
