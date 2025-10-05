package dev.ked.quetzalmap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listens for player join events to send SSE updates.
 */
public class PlayerJoinListener implements Listener {
    private final PlayerTracker playerTracker;

    public PlayerJoinListener(PlayerTracker playerTracker) {
        this.playerTracker = playerTracker;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerTracker.onPlayerJoin(event.getPlayer());
    }
}
