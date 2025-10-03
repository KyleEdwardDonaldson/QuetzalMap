package dev.ked.quetzalmap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player disconnect events to send SSE updates.
 */
public class PlayerQuitListener implements Listener {
    private final PlayerTracker playerTracker;

    public PlayerQuitListener(PlayerTracker playerTracker) {
        this.playerTracker = playerTracker;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerTracker.onPlayerQuit(event.getPlayer());
    }
}
