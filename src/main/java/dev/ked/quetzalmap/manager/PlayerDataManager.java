package dev.ked.quetzalmap.manager;

import dev.ked.quetzalmap.QuetzalMapPlugin;
import dev.ked.quetzalmap.model.PlayerMapData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final QuetzalMapPlugin plugin;
    private final Map<UUID, PlayerMapData> playerData;

    public PlayerDataManager(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
    }

    public PlayerMapData getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(),
            uuid -> new PlayerMapData(player.getUniqueId()));
    }

    public void removePlayerData(UUID uuid) {
        playerData.remove(uuid);
    }

    public void savePlayerData(Player player) {
        // In a more complete implementation, this would save to a file or database
        // For now, data is kept in memory only
    }

    public void loadPlayerData(Player player) {
        // Load player data from storage if it exists
        PlayerMapData data = getPlayerData(player);

        // Set default zoom level from config
        int defaultZoom = plugin.getConfig().getInt("map.default-zoom", 1024);
        data.setZoomLevel(defaultZoom);
    }
}
