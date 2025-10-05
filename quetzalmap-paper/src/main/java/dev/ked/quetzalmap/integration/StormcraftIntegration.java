package dev.ked.quetzalmap.integration;

import dev.ked.quetzalmap.server.sse.SSEManager;
import dev.ked.stormcraft.StormcraftPlugin;
import dev.ked.stormcraft.model.StormPhase;
import dev.ked.stormcraft.model.StormType;
import dev.ked.stormcraft.model.TravelingStorm;
import dev.ked.stormcraft.schedule.StormManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.logging.Logger;

/**
 * Integration with Stormcraft to broadcast storm positions and data to web map.
 * Handles multiple active storms and broadcasts updates via SSE.
 */
public class StormcraftIntegration {
    private static final Logger LOGGER = Logger.getLogger(StormcraftIntegration.class.getName());

    private final SSEManager sseManager;
    private final Plugin plugin;
    private StormManager stormManager;
    private BukkitRunnable updateTask;

    public StormcraftIntegration(SSEManager sseManager, Plugin plugin) {
        this.sseManager = sseManager;
        this.plugin = plugin;
    }

    /**
     * Initialize integration with Stormcraft.
     * Gets reference to StormManager and starts update task.
     */
    public boolean initialize() {
        Plugin stormcraft = Bukkit.getPluginManager().getPlugin("Stormcraft");
        if (stormcraft == null || !stormcraft.isEnabled()) {
            LOGGER.warning("Stormcraft plugin not found or not enabled");
            return false;
        }

        if (!(stormcraft instanceof StormcraftPlugin)) {
            LOGGER.warning("Stormcraft plugin is not the expected type");
            return false;
        }

        // Get storm manager from Stormcraft
        StormcraftPlugin stormcraftPlugin = (StormcraftPlugin) stormcraft;
        this.stormManager = stormcraftPlugin.getStormManager();

        if (stormManager == null) {
            LOGGER.warning("Failed to get StormManager from Stormcraft");
            return false;
        }

        // Start update task (runs every second to broadcast storm positions)
        startUpdateTask();

        LOGGER.info("Stormcraft integration initialized successfully");
        return true;
    }

    /**
     * Start periodic update task to broadcast storm data.
     * Runs every 20 ticks (1 second) to match Stormcraft's update rate.
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                broadcastStormUpdates();
            }
        };

        // Run every 20 ticks (1 second)
        updateTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Broadcast all active storm data to connected web map clients.
     */
    private void broadcastStormUpdates() {
        List<TravelingStorm> activeStorms = stormManager.getActiveStorms();

        if (activeStorms.isEmpty()) {
            // No storms - broadcast empty list
            sseManager.broadcast("storm_update", "{\"storms\":[]}");
            return;
        }

        // Build JSON array of all active storms
        StringBuilder json = new StringBuilder("{\"storms\":[");

        boolean first = true;
        for (TravelingStorm storm : activeStorms) {
            if (!first) json.append(",");
            first = false;

            json.append(buildStormJson(storm));
        }

        json.append("]}");

        // Broadcast to all connected clients
        sseManager.broadcast("storm_update", json.toString());
    }

    /**
     * Build JSON representation of a single storm.
     */
    private String buildStormJson(TravelingStorm storm) {
        Location center = storm.getCurrentLocation();
        Location target = storm.getTargetLocation();
        StormPhase phase = storm.getCurrentPhase();
        StormType type = storm.getProfile().getType();

        // Calculate unique storm ID based on start time and location
        String stormId = generateStormId(storm);

        return String.format(
            "{" +
            "\"id\":\"%s\"," +
            "\"x\":%.2f," +
            "\"z\":%.2f," +
            "\"targetX\":%.2f," +
            "\"targetZ\":%.2f," +
            "\"radius\":%.2f," +
            "\"baseRadius\":%.2f," +
            "\"radiusMultiplier\":%.3f," +
            "\"phase\":\"%s\"," +
            "\"phaseSymbol\":\"%s\"," +
            "\"phaseMultiplier\":%.3f," +
            "\"type\":\"%s\"," +
            "\"damage\":%.2f," +
            "\"speed\":%.2f," +
            "\"remainingSeconds\":%d," +
            "\"world\":\"%s\"" +
            "}",
            stormId,
            center.getX(),
            center.getZ(),
            target.getX(),
            target.getZ(),
            storm.getCurrentRadius(),
            storm.getDamageRadius(),
            storm.getRadiusMultiplier(),
            phase.name(),
            phase.getSymbol(),
            storm.getPhaseMultiplier(),
            type.name(),
            storm.getCurrentDamagePerSecond(),
            storm.getMovementSpeed(),
            storm.getRemainingSeconds(),
            center.getWorld().getName()
        );
    }

    /**
     * Generate a unique ID for a storm based on its start time.
     * This allows frontend to track the same storm across updates.
     */
    private String generateStormId(TravelingStorm storm) {
        return "storm_" + storm.getStartTimeMillis();
    }

    /**
     * Shutdown integration and cancel update task.
     */
    public void shutdown() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }
        LOGGER.info("Stormcraft integration shut down");
    }
}
