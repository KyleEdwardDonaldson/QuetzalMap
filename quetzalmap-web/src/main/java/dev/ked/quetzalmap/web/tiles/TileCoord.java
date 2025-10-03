package dev.ked.quetzalmap.web.tiles;

import java.util.Objects;

/**
 * Represents a tile coordinate in the map system.
 * Tiles are 512×512 pixel squares organized by world, zoom level, and x/z position.
 */
public final class TileCoord {
    private final String world;
    private final int zoom;
    private final int x;
    private final int z;
    private final int hash;

    public TileCoord(String world, int zoom, int x, int z) {
        this.world = world;
        this.zoom = zoom;
        this.x = x;
        this.z = z;
        this.hash = Objects.hash(world, zoom, x, z);
    }

    public String getWorld() {
        return world;
    }

    public int getZoom() {
        return zoom;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    /**
     * Get the filename for this tile (e.g., "0_0.png")
     */
    public String getFileName() {
        return x + "_" + z + ".png";
    }

    /**
     * Get the relative path for this tile (e.g., "world/0/0_0.png")
     */
    public String getRelativePath() {
        return world + "/" + zoom + "/" + getFileName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TileCoord)) return false;
        TileCoord that = (TileCoord) o;
        return zoom == that.zoom &&
               x == that.x &&
               z == that.z &&
               world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "TileCoord{world=" + world + ", zoom=" + zoom + ", x=" + x + ", z=" + z + "}";
    }
}
