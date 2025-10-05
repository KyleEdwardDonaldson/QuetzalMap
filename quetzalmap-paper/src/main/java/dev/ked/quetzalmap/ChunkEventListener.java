package dev.ked.quetzalmap;

import dev.ked.quetzalmap.web.tiles.TileManager;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Listens for chunk changes in Paper and marks tiles as dirty for re-rendering.
 * Uses standard Bukkit events for chunk tracking.
 */
public final class ChunkEventListener implements Listener {
    private final TileManager tileManager;
    private final BatchUpdateScheduler scheduler;

    // Track chunks that have been rendered to avoid re-rendering on every load
    private final Set<String> renderedChunks = new HashSet<>();

    public ChunkEventListener(TileManager tileManager, BatchUpdateScheduler scheduler) {
        this.tileManager = tileManager;
        this.scheduler = scheduler;
    }

    /**
     * Called when a chunk is loaded.
     * Render tiles as players explore to make the map visible.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Create a unique key for this chunk
        String chunkKey = world + ":" + chunkX + ":" + chunkZ;

        // Only render if we haven't rendered this chunk before
        if (renderedChunks.add(chunkKey)) {
            // Schedule chunk update (debounced)
            scheduler.scheduleChunkUpdate(world, chunkX, chunkZ);
        }
    }

    /**
     * Called when a chunk is populated (terrain generation).
     * We use MONITOR priority to ensure we run after all other plugins.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        Chunk chunk = event.getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Schedule chunk update (debounced)
        scheduler.scheduleChunkUpdate(world, chunkX, chunkZ);
    }

    /**
     * Called when a block is placed.
     * Track significant block changes for map updates.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        scheduler.scheduleChunkUpdate(world, chunkX, chunkZ);
    }

    /**
     * Called when a block is broken.
     * Track significant block changes for map updates.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        scheduler.scheduleChunkUpdate(world, chunkX, chunkZ);
    }
}
