package dev.ked.quetzalmap.web.tiles;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which tiles need to be re-rendered due to chunk changes.
 * Thread-safe for concurrent chunk updates.
 */
public final class DirtyTileTracker {
    private final Set<TileCoord> dirtyTiles;

    public DirtyTileTracker() {
        this.dirtyTiles = ConcurrentHashMap.newKeySet();
    }

    /**
     * Mark a tile as dirty (needs re-rendering).
     */
    public void markDirty(TileCoord coord) {
        dirtyTiles.add(coord);
    }

    /**
     * Mark multiple tiles as dirty.
     */
    public void markDirtyBatch(Set<TileCoord> coords) {
        dirtyTiles.addAll(coords);
    }

    /**
     * Mark all tiles affected by a chunk change as dirty.
     * A chunk can affect up to 4 tiles if it's on a boundary.
     */
    public void markChunkDirty(String world, int chunkX, int chunkZ, int zoom) {
        // Calculate which tile(s) this chunk belongs to
        // Tiles are 512×512 pixels, chunks are 16×16 blocks
        // So each tile contains 32×32 chunks

        int tileX = Math.floorDiv(chunkX, 32);
        int tileZ = Math.floorDiv(chunkZ, 32);

        markDirty(new TileCoord(world, zoom, tileX, tileZ));

        // Check if chunk is on tile boundary
        int chunkOffsetX = chunkX & 31; // chunkX % 32
        int chunkOffsetZ = chunkZ & 31; // chunkZ % 32

        // If on left edge, mark left neighbor
        if (chunkOffsetX == 0) {
            markDirty(new TileCoord(world, zoom, tileX - 1, tileZ));
        }

        // If on right edge, mark right neighbor
        if (chunkOffsetX == 31) {
            markDirty(new TileCoord(world, zoom, tileX + 1, tileZ));
        }

        // If on top edge, mark top neighbor
        if (chunkOffsetZ == 0) {
            markDirty(new TileCoord(world, zoom, tileX, tileZ - 1));
        }

        // If on bottom edge, mark bottom neighbor
        if (chunkOffsetZ == 31) {
            markDirty(new TileCoord(world, zoom, tileX, tileZ + 1));
        }

        // Corner cases (affects 4 tiles)
        if (chunkOffsetX == 0 && chunkOffsetZ == 0) {
            markDirty(new TileCoord(world, zoom, tileX - 1, tileZ - 1));
        }
        if (chunkOffsetX == 31 && chunkOffsetZ == 0) {
            markDirty(new TileCoord(world, zoom, tileX + 1, tileZ - 1));
        }
        if (chunkOffsetX == 0 && chunkOffsetZ == 31) {
            markDirty(new TileCoord(world, zoom, tileX - 1, tileZ + 1));
        }
        if (chunkOffsetX == 31 && chunkOffsetZ == 31) {
            markDirty(new TileCoord(world, zoom, tileX + 1, tileZ + 1));
        }
    }

    /**
     * Mark a tile as clean (rendered).
     */
    public void markClean(TileCoord coord) {
        dirtyTiles.remove(coord);
    }

    /**
     * Check if a tile is dirty.
     */
    public boolean isDirty(TileCoord coord) {
        return dirtyTiles.contains(coord);
    }

    /**
     * Get all dirty tiles and clear the tracker.
     */
    public Set<TileCoord> getDirtyAndClear() {
        Set<TileCoord> dirty = Set.copyOf(dirtyTiles);
        dirtyTiles.clear();
        return dirty;
    }

    /**
     * Get count of dirty tiles.
     */
    public int getDirtyCount() {
        return dirtyTiles.size();
    }

    /**
     * Clear all dirty tiles.
     */
    public void clear() {
        dirtyTiles.clear();
    }
}
