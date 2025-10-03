package dev.ked.quetzalmap.core.world;

/**
 * Minimal block type representation.
 * Contains only what we need for rendering.
 */
public class BlockType {
    private final String id;
    private final int color;
    private final boolean isAir;
    private final boolean isWater;
    private final boolean isTransparent;

    public BlockType(String id, int color) {
        this.id = id;
        this.color = color;
        this.isAir = id.contains("air");
        this.isWater = id.contains("water");
        this.isTransparent = id.contains("glass") || id.contains("ice");
    }

    public String getId() {
        return id;
    }

    public int getColor() {
        return color;
    }

    public boolean isAir() {
        return isAir;
    }

    public boolean isWater() {
        return isWater;
    }

    public boolean isTransparent() {
        return isTransparent;
    }
}
