package dev.ked.quetzalmap.web.rendering;

import dev.ked.quetzalmap.core.world.BlockState;
import dev.ked.quetzalmap.core.world.MinecraftChunk;
import dev.ked.quetzalmap.core.world.MinecraftRegion;
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
        this.pixelPool = new ChunkPixelDataPool(256); // Pool of 256 chunk pixel buffers
    }

    /**
     * Render a full tile from region file data.
     * This is used for initial renders and background updates.
     */
    public Tile renderFullTile(TileCoord coord, Path worldDirectory) {
        LOGGER.info("=== RENDERING TILE: coord=" + coord + ", worldDir=" + worldDirectory + " ===");
        Tile tile = new Tile(coord);

        // Calculate which region files we need
        // Each tile is 512 pixels = 32 chunks
        // Each region file is 32×32 chunks
        // So each tile = 1 region file

        int regionX = coord.getX();
        int regionZ = coord.getZ();

        LOGGER.info("Calculated region coordinates: regionX=" + regionX + ", regionZ=" + regionZ);

        try {
            Path regionFile = worldDirectory.resolve("region")
                    .resolve("r." + regionX + "." + regionZ + ".mca");

            LOGGER.info("Looking for region file: " + regionFile);

            if (!regionFile.toFile().exists()) {
                LOGGER.warning("Region file does NOT exist: " + regionFile);
                // Region doesn't exist - return empty tile
                return tile;
            }

            LOGGER.info("Region file EXISTS, loading...");
            // Load region
            MinecraftRegion region = new MinecraftRegion(regionFile, regionX, regionZ);
            LOGGER.info("MinecraftRegion created, rendering to tile...");
            renderRegionToTile(region, tile);

            tile.markClean();
            LOGGER.info("Tile rendering complete");
        } catch (Exception e) {
            LOGGER.severe("ERROR rendering tile: " + e.getMessage());
            e.printStackTrace();
        }

        return tile;
    }

    /**
     * Render a single chunk and update the tile incrementally.
     * This is used for real-time updates when chunks change.
     */
    public void renderChunkToTile(Tile tile, MinecraftRegion region, int chunkX, int chunkZ) {
        LOGGER.info("Rendering chunk: chunkX=" + chunkX + ", chunkZ=" + chunkZ);
        ChunkPixelData pixelData = pixelPool.acquire();
        try {
            // Get chunk from region
            LOGGER.info("Calling region.getChunk(" + chunkX + ", " + chunkZ + ")");
            MinecraftChunk chunk = region.getChunk(chunkX, chunkZ);
            if (chunk == null) {
                LOGGER.warning("Chunk is NULL for chunkX=" + chunkX + ", chunkZ=" + chunkZ);
                return;
            }

            LOGGER.info("Chunk loaded successfully, extracting block data...");
            // Calculate chunk position within tile (0-31)
            int chunkTileX = chunkX & 31;
            int chunkTileZ = chunkZ & 31;

            pixelData.setChunkCoords(chunkTileX, chunkTileZ);

            // Render each block in the chunk
            int nonNullBlocks = 0;
            for (int blockZ = 0; blockZ < 16; blockZ++) {
                for (int blockX = 0; blockX < 16; blockX++) {
                    BlockState blockState = chunk.getBlock(blockX, blockZ);
                    if (blockState != null) {
                        nonNullBlocks++;
                    }
                    int color = ColorCalculator.calculatePixelColor(blockState);
                    pixelData.setPixel(blockX, blockZ, color);
                }
            }

            LOGGER.info("Chunk had " + nonNullBlocks + " non-null blocks out of 256");
            // Update tile with rendered chunk pixels
            tile.updateChunkPixels(chunkTileX, chunkTileZ, pixelData.getPixels());

        } catch (Exception e) {
            LOGGER.severe("ERROR rendering chunk: " + e.getMessage());
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
            LOGGER.warning("Region is NULL, cannot render");
            // Empty region - fill with blank
            return;
        }

        LOGGER.info("Rendering region to tile: regionX=" + region.getRegionX() + ", regionZ=" + region.getRegionZ());
        // Iterate through all 32×32 chunks in the region
        int chunksRendered = 0;
        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                int regionChunkX = (region.getRegionX() << 5) + chunkX;
                int regionChunkZ = (region.getRegionZ() << 5) + chunkZ;

                renderChunkToTile(tile, region, regionChunkX, regionChunkZ);
                chunksRendered++;
            }
        }
        LOGGER.info("Finished rendering region: " + chunksRendered + " chunks processed");
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
