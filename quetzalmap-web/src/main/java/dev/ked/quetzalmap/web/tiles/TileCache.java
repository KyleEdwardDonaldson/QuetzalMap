package dev.ked.quetzalmap.web.tiles;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Three-tier cache system for map tiles:
 * - Hot cache: Frequently accessed tiles (viewport)
 * - Warm cache: Recently rendered tiles (background)
 * - Cold storage: Disk-based tiles (via TileStorage)
 *
 * Performance optimized with automatic promotion/demotion.
 */
public final class TileCache {
    private final Cache<TileCoord, Tile> hotCache;
    private final Cache<TileCoord, Tile> warmCache;
    private final TileStorage storage;

    // Statistics
    private final AtomicLong hotHits = new AtomicLong();
    private final AtomicLong warmHits = new AtomicLong();
    private final AtomicLong coldHits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public TileCache(TileStorage storage) {
        this(storage, 500, 2000);
    }

    public TileCache(TileStorage storage, int hotSize, int warmSize) {
        this.storage = storage;

        // Hot cache - viewport tiles, very fast access
        this.hotCache = Caffeine.newBuilder()
                .maximumSize(hotSize)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats()
                .build();

        // Warm cache - background tiles, evict to disk
        this.warmCache = Caffeine.newBuilder()
                .maximumSize(warmSize)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .evictionListener(this::onWarmEviction)
                .recordStats()
                .build();
    }

    /**
     * Get a tile from cache, loading from disk if necessary.
     * Automatically promotes warm → hot on access.
     */
    public Tile get(TileCoord coord) {
        // Check hot cache first
        Tile tile = hotCache.getIfPresent(coord);
        if (tile != null) {
            hotHits.incrementAndGet();
            return tile;
        }

        // Check warm cache
        tile = warmCache.getIfPresent(coord);
        if (tile != null) {
            warmHits.incrementAndGet();
            // Promote to hot cache
            promoteToHot(coord, tile);
            return tile;
        }

        // Try loading from disk
        tile = storage.load(coord);
        if (tile != null) {
            coldHits.incrementAndGet();
            // Put in warm cache
            warmCache.put(coord, tile);
            return tile;
        }

        // Cache miss - tile doesn't exist
        misses.incrementAndGet();
        return null;
    }

    /**
     * Put a tile in the cache (starts in hot cache).
     */
    public void put(TileCoord coord, Tile tile) {
        hotCache.put(coord, tile);
    }

    /**
     * Put a tile directly in warm cache (for background renders).
     */
    public void putWarm(TileCoord coord, Tile tile) {
        warmCache.put(coord, tile);
    }

    /**
     * Invalidate a tile (remove from all caches).
     */
    public void invalidate(TileCoord coord) {
        hotCache.invalidate(coord);
        warmCache.invalidate(coord);
        storage.delete(coord);
    }

    /**
     * Check if tile exists in hot cache.
     */
    public boolean isInHotCache(TileCoord coord) {
        return hotCache.getIfPresent(coord) != null;
    }

    /**
     * Check if tile exists in warm cache.
     */
    public boolean isInWarmCache(TileCoord coord) {
        return warmCache.getIfPresent(coord) != null;
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        long total = hotHits.get() + warmHits.get() + coldHits.get() + misses.get();
        double hitRate = total > 0 ? (double) (hotHits.get() + warmHits.get() + coldHits.get()) / total : 0.0;

        return new CacheStats(
                hotCache.estimatedSize(),
                warmCache.estimatedSize(),
                hotHits.get(),
                warmHits.get(),
                coldHits.get(),
                misses.get(),
                hitRate
        );
    }

    /**
     * Get memory usage estimate in bytes.
     */
    public long getMemoryUsage() {
        long hotMemory = hotCache.estimatedSize() * Tile.TILE_SIZE * Tile.TILE_SIZE * 4;
        long warmMemory = warmCache.estimatedSize() * Tile.TILE_SIZE * Tile.TILE_SIZE * 4;
        return hotMemory + warmMemory;
    }

    /**
     * Clear all caches (keeping disk storage).
     */
    public void clearCaches() {
        hotCache.invalidateAll();
        warmCache.invalidateAll();
        hotHits.set(0);
        warmHits.set(0);
        coldHits.set(0);
        misses.set(0);
    }

    private void promoteToHot(TileCoord coord, Tile tile) {
        hotCache.put(coord, tile);
        // Don't remove from warm - let it naturally evict
    }

    private void onWarmEviction(TileCoord coord, Tile tile, RemovalCause cause) {
        // Save to disk when evicted from warm cache
        if (cause.wasEvicted() && tile != null && tile.isDirty()) {
            storage.save(tile);
        }
    }

    /**
     * Cache statistics snapshot.
     */
    public record CacheStats(
            long hotSize,
            long warmSize,
            long hotHits,
            long warmHits,
            long coldHits,
            long misses,
            double hitRate
    ) {
        public long getTotalHits() {
            return hotHits + warmHits + coldHits;
        }

        public long getTotalRequests() {
            return hotHits + warmHits + coldHits + misses;
        }

        @Override
        public String toString() {
            return String.format(
                    "CacheStats{hot=%d, warm=%d, hits=%d/%d, hitRate=%.2f%%}",
                    hotSize, warmSize, getTotalHits(), getTotalRequests(), hitRate * 100
            );
        }
    }
}
