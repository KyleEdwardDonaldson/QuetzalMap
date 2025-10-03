package dev.ked.quetzalmap.web.tiles;

import dev.ked.quetzalmap.web.rendering.TileRenderer;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central coordinator for tile management.
 * Handles caching, rendering, and storage of map tiles.
 */
public final class TileManager {
    private static final Logger LOGGER = Logger.getLogger(TileManager.class.getName());

    private final TileCache cache;
    private final TileStorage storage;
    private final TileRenderer renderer;
    private final DirtyTileTracker dirtyTracker;
    private final ExecutorService renderExecutor;

    public TileManager(Path tilesDirectory) {
        this.storage = new TileStorage(tilesDirectory);
        this.cache = new TileCache(storage);
        this.renderer = new TileRenderer();
        this.dirtyTracker = new DirtyTileTracker();
        this.renderExecutor = Executors.newFixedThreadPool(4,
                r -> {
                    Thread t = new Thread(r, "TileRenderer");
                    t.setDaemon(true);
                    return t;
                });

        LOGGER.info("TileManager initialized with tiles directory: " + tilesDirectory);
    }

    /**
     * Get a tile, loading from cache or disk, or rendering if necessary.
     */
    public CompletableFuture<Tile> getTile(TileCoord coord, Path worldDirectory) {
        // Check cache first
        Tile tile = cache.get(coord);
        if (tile != null) {
            return CompletableFuture.completedFuture(tile);
        }

        // Tile doesn't exist - need to render
        return renderTile(coord, worldDirectory);
    }

    /**
     * Render a tile asynchronously.
     */
    public CompletableFuture<Tile> renderTile(TileCoord coord, Path worldDirectory) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            // Render the tile
            Tile tile = renderer.renderFullTile(coord, worldDirectory);

            // Put in cache
            cache.put(coord, tile);

            // Save to disk
            storage.save(tile);

            long renderTime = System.currentTimeMillis() - startTime;
            LOGGER.fine(String.format("Rendered tile %s in %dms", coord, renderTime));

            // Mark as clean in tracker
            dirtyTracker.markClean(coord);

            return tile;
        }, renderExecutor);
    }

    /**
     * Mark a chunk as changed, triggering incremental updates.
     */
    public void markChunkDirty(String world, int chunkX, int chunkZ, int zoom) {
        dirtyTracker.markChunkDirty(world, chunkX, chunkZ, zoom);
    }

    /**
     * Process all dirty tiles (render incrementally).
     */
    public CompletableFuture<Void> processDirtyTiles(Path worldDirectory) {
        var dirtyTiles = dirtyTracker.getDirtyAndClear();

        if (dirtyTiles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.info("Processing " + dirtyTiles.size() + " dirty tiles");

        var futures = dirtyTiles.stream()
                .map(coord -> renderTile(coord, worldDirectory))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    /**
     * Invalidate a tile (remove from cache and disk).
     */
    public void invalidateTile(TileCoord coord) {
        cache.invalidate(coord);
        dirtyTracker.markClean(coord);
    }

    /**
     * Get cache statistics.
     */
    public TileCache.CacheStats getCacheStats() {
        return cache.getStats();
    }

    /**
     * Get dirty tile count.
     */
    public int getDirtyCount() {
        return dirtyTracker.getDirtyCount();
    }

    /**
     * Get memory usage estimate in bytes.
     */
    public long getMemoryUsage() {
        return cache.getMemoryUsage();
    }

    /**
     * Shutdown the tile manager.
     */
    public void shutdown() {
        LOGGER.info("Shutting down TileManager");
        renderExecutor.shutdown();
        cache.clearCaches();
    }

    public TileCache getCache() {
        return cache;
    }

    public TileStorage getStorage() {
        return storage;
    }

    public DirtyTileTracker getDirtyTracker() {
        return dirtyTracker;
    }
}
