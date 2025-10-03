package dev.ked.quetzalmap.manager;

import dev.ked.quetzalmap.QuetzalMapPlugin;
import dev.ked.quetzalmap.renderer.QuetzalMapRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MapManager {

    private final QuetzalMapPlugin plugin;
    private final Map<UUID, MapView> playerMaps;

    public MapManager(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
        this.playerMaps = new HashMap<>();
    }

    public boolean giveMapToPlayer(Player player) {
        // Check if player already has a QuetzalMap
        if (hasQuetzalMap(player)) {
            player.sendMessage("§cYou already have a QuetzalMap!");
            return false;
        }

        // Create map view
        MapView mapView = getOrCreateMapView(player);

        // Create map item
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapView(mapView);
        meta.setDisplayName("§6QuetzalMap");
        mapItem.setItemMeta(meta);

        // Give to player
        player.getInventory().addItem(mapItem);
        player.sendMessage("§aYou have received a QuetzalMap!");
        return true;
    }

    public MapView getOrCreateMapView(Player player) {
        MapView existingView = playerMaps.get(player.getUniqueId());
        if (existingView != null) {
            return existingView;
        }

        // Create new map view - this should automatically add default renderers
        MapView mapView = Bukkit.createMap(player.getWorld());

        // IMPORTANT: Set center BEFORE setting other properties
        mapView.setCenterX(player.getLocation().getBlockX());
        mapView.setCenterZ(player.getLocation().getBlockZ());

        // Set map properties
        mapView.setScale(MapView.Scale.FARTHEST); // Largest area view
        mapView.setUnlimitedTracking(true); // Track player outside map bounds
        mapView.setTrackingPosition(true); // Enable player position tracking
        mapView.setLocked(false); // Allow zoom changes if needed

        // Keep the default renderer for terrain discovery
        // Our renderer will add overlays on top
        QuetzalMapRenderer renderer = new QuetzalMapRenderer(plugin, player);
        mapView.addRenderer(renderer);

        // Start a task to update the map center as the player moves
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                mapView.setCenterX(player.getLocation().getBlockX());
                mapView.setCenterZ(player.getLocation().getBlockZ());
            }
        }, 10L, 10L); // Update every 0.5 seconds

        playerMaps.put(player.getUniqueId(), mapView);
        return mapView;
    }

    public boolean hasQuetzalMap(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.FILLED_MAP) {
                MapMeta meta = (MapMeta) item.getItemMeta();
                if (meta != null && meta.hasDisplayName() &&
                    meta.getDisplayName().equals("§6QuetzalMap")) {
                    return true;
                }
            }
        }
        return false;
    }

    public void removePlayerMap(UUID playerId) {
        playerMaps.remove(playerId);
    }

    public void shutdown() {
        playerMaps.clear();
    }
}
