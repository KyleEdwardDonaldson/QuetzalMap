package dev.ked.quetzalmap.web.pregen;

import dev.ked.quetzalmap.web.tiles.Tile;
import dev.ked.quetzalmap.web.tiles.TileCoord;
import dev.ked.quetzalmap.web.tiles.TileManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Pre-generates map tiles in the background to eliminate user-facing render delays.
 * Uses spiral pattern from center (spawn) outward for logical loading order.
 */
public class TilePreGenerator {
    private static final Logger LOGGER = Logger.getLogger(TilePreGenerator.class.getName());

    private final TileManager tileManager;
    private final ExecutorService pregenExecutor;
    private final AtomicInteger tilesGenerated = new AtomicInteger(0);
    private final AtomicInteger tilesSkipped = new AtomicInteger(0);
    private volatile boolean running = false;

    public TilePreGenerator(TileManager tileManager, int threadCount) {
        this.tileManager = tileManager;
        this.pregenExecutor = Executors.newFixedThreadPool(threadCount,
                r -> {
                    Thread t = new Thread(r, "TilePregen");
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY); // Low priority - don't interfere with live requests
                    return t;
                });

        LOGGER.info("TilePreGenerator initialized with " + threadCount + " threads");
    }

    /**
     * Pre-generate tiles in a radius around spawn/center.
     * Uses spiral pattern for logical loading order.
     *
     * @param worldDirectory World data directory
     * @param world World name
     * @param centerX Center tile X coordinate (usually 0 for spawn)
     * @param centerZ Center tile Z coordinate (usually 0 for spawn)
     * @param radiusTiles Radius in tiles to generate
     * @return CompletableFuture that completes when generation is done
     */
    public CompletableFuture<PregenStats> preGenerateArea(
            Path worldDirectory,
            String world,
            int centerX,
            int centerZ,
            int radiusTiles) {

        if (running) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Pre-generation already running"));
        }

        running = true;
        tilesGenerated.set(0);
        tilesSkipped.set(0);

        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting pre-generation: world=" + world +
                ", center=(" + centerX + "," + centerZ + ")" +
                ", radius=" + radiusTiles + " tiles");

        // Generate tile coordinates in spiral pattern
        List<TileCoord> coords = generateSpiralCoords(world, centerX, centerZ, radiusTiles);
        int totalTiles = coords.size();

        LOGGER.info("Pre-generating " + totalTiles + " tiles...");

        // Process tiles concurrently
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (TileCoord coord : coords) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Check if tile already exists
                    Tile existing = tileManager.getTile(coord, worldDirectory).get(30, TimeUnit.SECONDS);
                    if (existing != null && !existing.isDirty()) {
                        tilesSkipped.incrementAndGet();
                        return;
                    }

                    // Render tile
                    tileManager.renderTile(coord, worldDirectory).get(60, TimeUnit.SECONDS);
                    int generated = tilesGenerated.incrementAndGet();

                    // Log progress every 10 tiles
                    if (generated % 10 == 0) {
                        int percent = (generated * 100) / totalTiles;
                        LOGGER.info("Pre-generation progress: " + generated + "/" + totalTiles +
                                " (" + percent + "%) - " + tilesSkipped.get() + " skipped");
                    }

                } catch (TimeoutException e) {
                    LOGGER.warning("Tile render timeout: " + coord);
                } catch (Exception e) {
                    LOGGER.warning("Failed to pre-generate tile " + coord + ": " + e.getMessage());
                }
            }, pregenExecutor);

            futures.add(future);
        }

        // Wait for all tiles to complete
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .handle((result, error) -> {
                    running = false;
                    long duration = System.currentTimeMillis() - startTime;

                    PregenStats stats = new PregenStats(
                            totalTiles,
                            tilesGenerated.get(),
                            tilesSkipped.get(),
                            duration
                    );

                    LOGGER.info("Pre-generation complete: " +
                            "generated=" + stats.generated() +
                            ", skipped=" + stats.skipped() +
                            ", duration=" + (duration / 1000) + "s" +
                            ", rate=" + String.format("%.1f", stats.tilesPerSecond()) + " tiles/s");

                    return stats;
                });
    }

    /**
     * Generate tile coordinates in spiral pattern from center outward.
     * This ensures spawn area loads first, then expands logically.
     */
    private List<TileCoord> generateSpiralCoords(String world, int centerX, int centerZ, int radius) {
        List<TileCoord> coords = new ArrayList<>();

        // Start at center
        coords.add(new TileCoord(world, 0, centerX, centerZ));

        // Spiral outward
        for (int r = 1; r <= radius; r++) {
            // Right edge (moving up)
            for (int z = -r + 1; z <= r; z++) {
                coords.add(new TileCoord(world, 0, centerX + r, centerZ + z));
            }

            // Top edge (moving left)
            for (int x = r - 1; x >= -r; x--) {
                coords.add(new TileCoord(world, 0, centerX + x, centerZ + r));
            }

            // Left edge (moving down)
            for (int z = r - 1; z >= -r; z--) {
                coords.add(new TileCoord(world, 0, centerX - r, centerZ + z));
            }

            // Bottom edge (moving right)
            for (int x = -r + 1; x < r; x++) {
                coords.add(new TileCoord(world, 0, centerX + x, centerZ - r));
            }
        }

        return coords;
    }

    /**
     * Stop pre-generation (graceful shutdown).
     */
    public void stop() {
        LOGGER.info("Stopping pre-generation...");
        running = false;
        pregenExecutor.shutdown();
        try {
            if (!pregenExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                pregenExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            pregenExecutor.shutdownNow();
        }
        LOGGER.info("Pre-generation stopped");
    }

    /**
     * Check if pre-generation is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get current pre-generation statistics.
     */
    public PregenStats getCurrentStats() {
        return new PregenStats(
                0, // total unknown mid-run
                tilesGenerated.get(),
                tilesSkipped.get(),
                0 // duration unknown mid-run
        );
    }

    /**
     * Statistics for pre-generation run.
     */
    public record PregenStats(
            int totalTiles,
            int generated,
            int skipped,
            long durationMs
    ) {
        public double tilesPerSecond() {
            if (durationMs == 0) return 0;
            return (generated * 1000.0) / durationMs;
        }

        @Override
        public String toString() {
            return String.format("PregenStats[total=%d, generated=%d, skipped=%d, duration=%dms, rate=%.1f tiles/s]",
                    totalTiles, generated, skipped, durationMs, tilesPerSecond());
        }
    }
}
