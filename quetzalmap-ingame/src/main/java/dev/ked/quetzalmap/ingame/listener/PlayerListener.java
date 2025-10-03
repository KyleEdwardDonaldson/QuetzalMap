package dev.ked.quetzalmap.listener;

import dev.ked.quetzalmap.QuetzalMapPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final QuetzalMapPlugin plugin;

    public PlayerListener(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer());

        // Give map if configured
        if (plugin.getConfig().getBoolean("map.auto-give-on-join", true)) {
            // Delay giving map by 1 tick to ensure player is fully loaded
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Only give if they don't already have one
                if (!plugin.getMapManager().hasQuetzalMap(event.getPlayer())) {
                    plugin.getMapManager().giveMapToPlayer(event.getPlayer());
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data
        plugin.getPlayerDataManager().savePlayerData(event.getPlayer());

        // Clean up map data
        plugin.getMapManager().removePlayerMap(event.getPlayer().getUniqueId());
    }
}
