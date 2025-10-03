package dev.ked.quetzalmap.integration;

import dev.ked.quetzalmap.QuetzalMapPlugin;
import dev.ked.quetzalmap.model.Marker;
import dev.ked.quetzalmap.model.MarkerType;
import dev.ked.stormcraft.StormcraftPlugin;
import dev.ked.stormcraft.model.TravelingStorm;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.map.MapCursor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class StormcraftIntegration implements Integration {

    private final QuetzalMapPlugin plugin;
    private StormcraftPlugin stormcraftPlugin;
    private BukkitTask updateTask;
    private boolean initialized = false;

    public StormcraftIntegration(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean initialize() {
        try {
            // Get Stormcraft plugin instance
            Plugin stormcraftBukkit = Bukkit.getPluginManager().getPlugin("Stormcraft");
            if (stormcraftBukkit == null || !(stormcraftBukkit instanceof StormcraftPlugin)) {
                plugin.getLogger().warning("Stormcraft plugin not found");
                return false;
            }

            stormcraftPlugin = (StormcraftPlugin) stormcraftBukkit;

            // Start periodic marker updates
            int updateInterval = plugin.getConfig().getInt("integrations.stormcraft.update-interval", 20);
            updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateMarkers, 20L, updateInterval);

            initialized = true;
            plugin.getLogger().info("Stormcraft integration initialized");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize Stormcraft integration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Clear all storm markers
        plugin.getMarkerManager().clearMarkersByType(MarkerType.STORM.getConfigKey());
        initialized = false;
    }

    @Override
    public String getName() {
        return "Stormcraft";
    }

    @Override
    public void updateMarkers() {
        if (!initialized || !plugin.getConfig().getBoolean("markers.storms.enabled", true)) {
            return;
        }

        // Clear existing storm markers
        plugin.getMarkerManager().clearMarkersByType(MarkerType.STORM.getConfigKey());

        // Get storm manager
        dev.ked.stormcraft.schedule.StormManager stormManager = stormcraftPlugin.getStormManager();
        if (stormManager == null) {
            return;
        }

        // Get all active traveling storms
        List<TravelingStorm> storms = stormManager.getActiveStorms();
        if (storms == null || storms.isEmpty()) {
            return;
        }

        // Create markers for each storm
        for (TravelingStorm storm : storms) {
            Location location = storm.getCurrentLocation();
            Location targetLocation = storm.getTargetLocation();
            double radius = storm.getDamageRadius();
            double speed = storm.getMovementSpeed();

            String markerId = Marker.generateId("storm");
            Marker marker = new Marker(
                markerId,
                MarkerType.STORM.getConfigKey(),
                location,
                storm.getProfile().getType().name()
            );

            // Store storm data in marker for rendering
            marker.setData("radius", radius);
            marker.setData("targetX", targetLocation.getX());
            marker.setData("targetZ", targetLocation.getZ());
            marker.setData("speed", speed);
            marker.setData("remaining", storm.getRemainingSeconds());

            marker.setCursorType(MapCursor.Type.RED_X);
            plugin.getMarkerManager().addMarker(marker);
        }

        if (!storms.isEmpty()) {
            plugin.getLogger().fine("Updated " + storms.size() + " storm markers");
        }
    }
}
