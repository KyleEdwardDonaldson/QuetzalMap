package dev.ked.quetzalmap;

import dev.ked.quetzalmap.server.sse.SSEManager;
import dev.ked.quetzalmap.web.tiles.TileCoord;
import dev.ked.quetzalmap.web.tiles.TileManager;
import dev.ked.quetzalmap.web.world.WorldAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Debounces chunk updates to avoid re-rendering the same tile multiple times.
 * Collects dirty chunks for 100ms, then batch processes them.
 */
public final class BatchUpdateScheduler {
    private static final Logger LOGGER = Logger.getLogger(BatchUpdateScheduler.class.getName());
    private static final long BATCH_INTERVAL_TICKS = 2; // 100ms = 2 ticks

    private final Plugin plugin;
    private final TileManager tileManager;
    private final WorldAdapter worldAdapter;
    private final SSEManager sseManager;
    private final Set<ChunkUpdate> pendingUpdates;
    private int taskId = -1;

    public BatchUpdateScheduler(Plugin plugin, TileManager tileManager, WorldAdapter worldAdapter, SSEManager sseManager) {
        this.plugin = plugin;
        this.tileManager = tileManager;
        this.worldAdapter = worldAdapter;
        this.sseManager = sseManager;
        this.pendingUpdates = ConcurrentHashMap.newKeySet();
    }

    /**
     * Start the scheduler.
     */
    public void start() {
        if (taskId != -1) {
            LOGGER.warning("BatchUpdateScheduler already running");
            return;
        }

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::processBatch,
                BATCH_INTERVAL_TICKS, BATCH_INTERVAL_TICKS).getTaskId();

        LOGGER.info("BatchUpdateScheduler started (interval: " + BATCH_INTERVAL_TICKS + " ticks)");
    }

    /**
     * Stop the scheduler.
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
            LOGGER.info("BatchUpdateScheduler stopped");
        }

        // Process any remaining updates
        if (!pendingUpdates.isEmpty()) {
            LOGGER.info("Processing " + pendingUpdates.size() + " remaining updates");
            processBatch();
        }
    }

    /**
     * Schedule a chunk update (debounced).
     */
    public void scheduleChunkUpdate(String worldName, int chunkX, int chunkZ) {
        pendingUpdates.add(new ChunkUpdate(worldName, chunkX, chunkZ));
    }

    /**
     * Process all pending updates.
     */
    private void processBatch() {
        if (pendingUpdates.isEmpty()) {
            return;
        }

        // Collect all pending updates
        Set<ChunkUpdate> updates = Set.copyOf(pendingUpdates);
        pendingUpdates.clear();

        LOGGER.fine("Processing batch of " + updates.size() + " chunk updates");

        // Mark all affected tiles as dirty
        for (ChunkUpdate update : updates) {
            World world = Bukkit.getWorld(update.worldName);
            if (world == null) {
                LOGGER.warning("World not found: " + update.worldName);
                continue;
            }

            // Mark chunk dirty for all zoom levels
            // TODO: Make zoom levels configurable
            for (int zoom = 0; zoom <= 3; zoom++) {
                tileManager.markChunkDirty(update.worldName, update.chunkX, update.chunkZ, zoom);
            }
        }

        // Process dirty tiles asynchronously
        int dirtyCount = tileManager.getDirtyCount();
        if (dirtyCount > 0) {
            LOGGER.fine("Processing " + dirtyCount + " dirty tiles");

            // Get world directory for rendering
            // Note: Assumes single world for now - TODO: multi-world support
            World world = Bukkit.getWorlds().get(0);
            Path worldDir = worldAdapter.getWorldDirectory(world);

            // Get dirty tiles before processing
            Set<TileCoord> dirtyTiles = tileManager.getDirtyTracker().getDirtyAndClear();

            tileManager.processDirtyTiles(worldDir).thenRun(() -> {
                LOGGER.fine("Batch processing complete");

                // Broadcast tile updates to SSE clients
                if (sseManager != null) {
                    for (TileCoord coord : dirtyTiles) {
                        sseManager.broadcastTileUpdate(coord.getWorld(), coord.getZoom(),
                                coord.getX(), coord.getZ());
                    }
                }
            }).exceptionally(e -> {
                LOGGER.severe("Error processing dirty tiles: " + e.getMessage());
                e.printStackTrace();
                return null;
            });
        }
    }

    /**
     * Get pending update count.
     */
    public int getPendingCount() {
        return pendingUpdates.size();
    }

    /**
     * Chunk update record.
     */
    private record ChunkUpdate(String worldName, int chunkX, int chunkZ) {
    }
}
