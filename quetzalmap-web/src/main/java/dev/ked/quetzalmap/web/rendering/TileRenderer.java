package dev.ked.quetzalmap.web.rendering;

import dev.ked.quetzalmap.core.world.BlockState;
import dev.ked.quetzalmap.core.world.MinecraftChunk;
import dev.ked.quetzalmap.core.world.MinecraftRegion;
import dev.ked.quetzalmap.core.world.RegionCache;
import dev.ked.quetzalmap.web.pool.ChunkPixelData;
import dev.ked.quetzalmap.web.pool.ChunkPixelDataPool;
import dev.ked.quetzalmap.web.tiles.Tile;
import dev.ked.quetzalmap.web.tiles.TileCoord;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Renders map tiles from Minecraft world data.
 * Supports both full tile rendering and incremental chunk updates.
 */
public final class TileRenderer {
    private static final Logger LOGGER = Logger.getLogger(TileRenderer.class.getName());
    private final ChunkPixelDataPool pixelPool;

    public TileRenderer() {
        // Auto-scale pool size based on CPU cores
        // Each thread needs ~32 chunks in flight
        int cores = Runtime.getRuntime().availableProcessors();
        int renderThreads = Math.max(2, Math.min(cores - 2, 16));
        int poolSize = renderThreads * 64; // 64 chunks per thread

        this.pixelPool = new ChunkPixelDataPool(poolSize);
        LOGGER.info("TileRenderer initialized with pixel pool size: " + poolSize);
    }

    /**
     * Render a full tile from region file data.
     * This is used for initial renders and background updates.
     */
    public Tile renderFullTile(TileCoord coord, Path worldDirectory) {
        LOGGER.fine("Rendering tile: " + coord);
        Tile tile = new Tile(coord);

        // Calculate which region files we need
        // Each tile is 512 pixels = 32 chunks
        // Each region file is 32×32 chunks
        // So each tile = 1 region file

        int regionX = coord.getX();
        int regionZ = coord.getZ();

        try {
            Path regionFile = worldDirectory.resolve("region")
                    .resolve("r." + regionX + "." + regionZ + ".mca");

            if (!regionFile.toFile().exists()) {
                // Region doesn't exist - return empty tile
                return tile;
            }

            // Load region from cache (major performance optimization)
            // Avoids repeated I/O + NBT parsing for same region
            MinecraftRegion region = RegionCache.getInstance().getRegion(worldDirectory, regionX, regionZ);
            renderRegionToTile(region, tile);

            tile.markClean();
            LOGGER.fine("Tile rendering complete: " + coord);
        } catch (Exception e) {
            LOGGER.severe("ERROR rendering tile " + coord + ": " + e.getMessage());
            e.printStackTrace();
        }

        return tile;
    }

    /**
     * Render a single chunk and update the tile incrementally.
     * This is used for real-time updates when chunks change.
     */
    public void renderChunkToTile(Tile tile, MinecraftRegion region, int chunkX, int chunkZ) {
        ChunkPixelData pixelData = pixelPool.acquire();
        try {
            // Get chunk from region
            MinecraftChunk chunk = region.getChunk(chunkX, chunkZ);
            if (chunk == null) {
                return;
            }

            // Calculate chunk position within tile (0-31)
            int chunkTileX = chunkX & 31;
            int chunkTileZ = chunkZ & 31;

            pixelData.setChunkCoords(chunkTileX, chunkTileZ);

            // Render each block in the chunk
            for (int blockZ = 0; blockZ < 16; blockZ++) {
                for (int blockX = 0; blockX < 16; blockX++) {
                    BlockState blockState = chunk.getBlock(blockX, blockZ);
                    int color = ColorCalculator.calculatePixelColor(blockState);
                    pixelData.setPixel(blockX, blockZ, color);
                }
            }

            // Update tile with rendered chunk pixels
            tile.updateChunkPixels(chunkTileX, chunkTileZ, pixelData.getPixels());

        } catch (Exception e) {
            LOGGER.severe("ERROR rendering chunk (" + chunkX + "," + chunkZ + "): " + e.getMessage());
            e.printStackTrace();
        } finally {
            pixelPool.release(pixelData);
        }
    }

    /**
     * Render an entire region to a tile.
     * Internal method used by renderFullTile.
     */
    private void renderRegionToTile(MinecraftRegion region, Tile tile) {
        if (region == null) {
            return;
        }

        // Iterate through all 32×32 chunks in the region
        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                int regionChunkX = (region.getRegionX() << 5) + chunkX;
                int regionChunkZ = (region.getRegionZ() << 5) + chunkZ;

                renderChunkToTile(tile, region, regionChunkX, regionChunkZ);
            }
        }
    }

    /**
     * Get rendering statistics.
     */
    public RenderStats getStats() {
        return new RenderStats(
                pixelPool.getSize(),
                pixelPool.getMaxSize(),
                pixelPool.isExhausted()
        );
    }

    public record RenderStats(
            int poolSize,
            int poolMax,
            boolean poolExhausted
    ) {
        @Override
        public String toString() {
            return String.format("RenderStats{pool=%d/%d, exhausted=%s}",
                    poolSize, poolMax, poolExhausted);
        }
    }
}
