package dev.ked.quetzalmap;

import dev.ked.quetzalmap.server.sse.SSEManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Tracks player positions and broadcasts updates via SSE.
 * Updates sent every second for all online players.
 */
public class PlayerTracker extends BukkitRunnable {
    private static final Logger LOGGER = Logger.getLogger(PlayerTracker.class.getName());

    private final QuetzalMapPlugin plugin;
    private final SSEManager sseManager;
    private final Map<UUID, PlayerPosition> lastPositions = new HashMap<>();

    // Minimum movement distance to trigger update (blocks)
    private static final double MIN_MOVEMENT = 5.0;

    public PlayerTracker(QuetzalMapPlugin plugin, SSEManager sseManager) {
        this.plugin = plugin;
        this.sseManager = sseManager;
    }

    public void start() {
        // Run every 20 ticks (1 second)
        this.runTaskTimerAsynchronously(plugin, 20L, 20L);
        LOGGER.info("PlayerTracker started - updating every 1 second");
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                Location loc = player.getLocation();
                UUID uuid = player.getUniqueId();
                PlayerPosition newPos = new PlayerPosition(
                        loc.getX(),
                        loc.getY(),
                        loc.getZ(),
                        loc.getYaw(),
                        loc.getWorld().getName()
                );

                PlayerPosition lastPos = lastPositions.get(uuid);

                // Only send update if player moved significantly or world changed
                if (lastPos == null || shouldUpdate(lastPos, newPos)) {
                    sendPlayerUpdate(player, newPos);
                    lastPositions.put(uuid, newPos);
                }

            } catch (Exception e) {
                LOGGER.warning("Error tracking player " + player.getName() + ": " + e.getMessage());
            }
        }

        // Clean up disconnected players
        lastPositions.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    /**
     * Check if position update should be sent.
     */
    private boolean shouldUpdate(PlayerPosition last, PlayerPosition now) {
        // Different world = always update
        if (!last.world.equals(now.world)) {
            return true;
        }

        // Calculate distance moved
        double dx = now.x - last.x;
        double dy = now.y - last.y;
        double dz = now.z - last.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Update if moved > MIN_MOVEMENT blocks
        return distance >= MIN_MOVEMENT;
    }

    /**
     * Send player position update via SSE.
     */
    private void sendPlayerUpdate(Player player, PlayerPosition pos) {
        String json = String.format(
                "{\"uuid\":\"%s\",\"name\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"yaw\":%.2f,\"world\":\"%s\"}",
                player.getUniqueId(),
                player.getName(),
                pos.x,
                pos.y,
                pos.z,
                pos.yaw,
                pos.world
        );

        sseManager.broadcast("player_moved", json);
    }

    /**
     * Send player join event.
     */
    public void onPlayerJoin(Player player) {
        Location loc = player.getLocation();
        PlayerPosition pos = new PlayerPosition(
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getWorld().getName()
        );

        lastPositions.put(player.getUniqueId(), pos);

        String json = String.format(
                "{\"uuid\":\"%s\",\"name\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"yaw\":%.2f,\"world\":\"%s\"}",
                player.getUniqueId(),
                player.getName(),
                pos.x,
                pos.y,
                pos.z,
                pos.yaw,
                pos.world
        );

        sseManager.broadcast("player_join", json);
    }

    /**
     * Send player disconnect event.
     */
    public void onPlayerQuit(Player player) {
        lastPositions.remove(player.getUniqueId());

        String json = String.format(
                "{\"uuid\":\"%s\",\"name\":\"%s\"}",
                player.getUniqueId(),
                player.getName()
        );

        sseManager.broadcast("player_disconnect", json);
    }

    /**
     * Send initial player list to new SSE connection.
     * Must be called from Bukkit thread or will schedule itself.
     */
    public void sendInitialPlayerList() {
        // If not on main thread, schedule on main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::sendInitialPlayerList);
            return;
        }

        StringBuilder json = new StringBuilder("{\"players\":[");

        boolean first = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!first) json.append(",");
            first = false;

            Location loc = player.getLocation();
            json.append(String.format(
                    "{\"uuid\":\"%s\",\"name\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"yaw\":%.2f,\"world\":\"%s\"}",
                    player.getUniqueId(),
                    player.getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ(),
                    loc.getYaw(),
                    loc.getWorld().getName()
            ));
        }

        json.append("]}");

        LOGGER.fine("Broadcasting player_list: " + json.toString());
        sseManager.broadcast("player_list", json.toString());
    }

    /**
     * Immutable player position data.
     */
    private record PlayerPosition(double x, double y, double z, float yaw, String world) {}
}
