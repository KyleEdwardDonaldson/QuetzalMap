package dev.ked.quetzalmap.web.world;

import dev.ked.quetzalmap.core.world.MinecraftRegion;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridges Paper World objects to MinecraftRegion objects.
 * Handles world directory resolution and Region caching.
 */
public final class WorldAdapter {
    private static final Logger LOGGER = Logger.getLogger(WorldAdapter.class.getName());

    private final ConcurrentHashMap<RegionKey, MinecraftRegion> regionCache;
    private final Path serverWorldsDirectory;

    public WorldAdapter(Path serverWorldsDirectory) {
        this.serverWorldsDirectory = serverWorldsDirectory;
        this.regionCache = new ConcurrentHashMap<>();
    }

    /**
     * Get the world directory for a Bukkit World.
     */
    public Path getWorldDirectory(World world) {
        return serverWorldsDirectory.resolve(world.getName());
    }

    /**
     * Get a Region object for the given world and region coordinates.
     * Regions are cached for performance.
     */
    public MinecraftRegion getRegion(World world, int regionX, int regionZ) {
        RegionKey key = new RegionKey(world.getName(), regionX, regionZ);

        // Check cache first
        MinecraftRegion cached = regionCache.get(key);
        if (cached != null) {
            return cached;
        }

        // Load region from disk
        Path worldDir = getWorldDirectory(world);
        Path regionFile = worldDir.resolve("region")
                .resolve("r." + regionX + "." + regionZ + ".mca");

        if (!regionFile.toFile().exists()) {
            LOGGER.fine(String.format("Region file does not exist: %s", regionFile));
            return null;
        }

        MinecraftRegion region = new MinecraftRegion(regionFile, regionX, regionZ);
        regionCache.put(key, region);
        return region;
    }

    /**
     * Get a Region for the given chunk coordinates.
     */
    public MinecraftRegion getRegionForChunk(World world, int chunkX, int chunkZ) {
        int regionX = chunkX >> 5; // chunkX / 32
        int regionZ = chunkZ >> 5; // chunkZ / 32
        return getRegion(world, regionX, regionZ);
    }

    /**
     * Invalidate cached region (useful when region file changes).
     */
    public void invalidateRegion(String worldName, int regionX, int regionZ) {
        RegionKey key = new RegionKey(worldName, regionX, regionZ);
        regionCache.remove(key);
    }

    /**
     * Clear all cached regions for a world.
     */
    public void clearWorld(String worldName) {
        regionCache.keySet().removeIf(key -> key.worldName.equals(worldName));
    }

    /**
     * Clear all cached regions.
     */
    public void clearAll() {
        regionCache.clear();
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        return new CacheStats(regionCache.size());
    }

    /**
     * Cache key for regions.
     */
    private record RegionKey(String worldName, int regionX, int regionZ) {
    }

    public record CacheStats(int cachedRegions) {
        @Override
        public String toString() {
            return String.format("WorldAdapter{cachedRegions=%d}", cachedRegions);
        }
    }
}
