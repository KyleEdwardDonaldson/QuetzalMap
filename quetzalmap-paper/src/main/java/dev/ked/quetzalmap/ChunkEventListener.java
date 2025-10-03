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

/**
 * Listens for chunk changes in Paper and marks tiles as dirty for re-rendering.
 * Uses standard Bukkit events for chunk tracking.
 */
public final class ChunkEventListener implements Listener {
    private final TileManager tileManager;
    private final BatchUpdateScheduler scheduler;

    public ChunkEventListener(TileManager tileManager, BatchUpdateScheduler scheduler) {
        this.tileManager = tileManager;
        this.scheduler = scheduler;
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
