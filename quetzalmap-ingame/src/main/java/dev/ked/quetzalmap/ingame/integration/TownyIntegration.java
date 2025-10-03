package dev.ked.quetzalmap.integration;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import dev.ked.quetzalmap.QuetzalMapPlugin;
import dev.ked.quetzalmap.model.Marker;
import dev.ked.quetzalmap.model.MarkerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.map.MapCursor;
import org.bukkit.scheduler.BukkitTask;

public class TownyIntegration implements Integration {

    private final QuetzalMapPlugin plugin;
    private BukkitTask updateTask;
    private boolean initialized = false;

    public TownyIntegration(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean initialize() {
        try {
            // Verify Towny is available
            TownyAPI.getInstance();

            // Start periodic marker updates
            int updateInterval = plugin.getConfig().getInt("integrations.towny.update-interval", 100);
            updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateMarkers, 20L, updateInterval);

            initialized = true;
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize Towny integration: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Clear all Towny markers
        plugin.getMarkerManager().clearMarkersByType(MarkerType.TOWN.getConfigKey());
        plugin.getMarkerManager().clearMarkersByType(MarkerType.NATION.getConfigKey());
        initialized = false;
    }

    @Override
    public String getName() {
        return "Towny";
    }

    @Override
    public void updateMarkers() {
        if (!initialized) {
            return;
        }

        updateTownMarkers();
        updateNationMarkers();
    }

    private void updateTownMarkers() {
        if (!plugin.getConfig().getBoolean("markers.towns.enabled", true)) {
            return;
        }

        // Clear existing town markers
        plugin.getMarkerManager().clearMarkersByType(MarkerType.TOWN.getConfigKey());

        int maxTowns = plugin.getConfig().getInt("markers.towns.max-towns", 100);
        boolean showSpawn = plugin.getConfig().getBoolean("markers.towns.show-spawn", true);
        int townCount = 0;

        TownyAPI api = TownyAPI.getInstance();

        for (Town town : api.getTowns()) {
            if (townCount >= maxTowns) {
                break;
            }

            try {
                Location location;
                if (showSpawn && town.hasSpawn()) {
                    // Use town spawn point
                    location = town.getSpawn();
                } else if (town.hasHomeBlock()) {
                    // Use homeblock location - convert TownBlock to Location
                    org.bukkit.World world = town.getHomeBlock().getWorld().getBukkitWorld();
                    int x = town.getHomeBlock().getX() * 16 + 8; // Center of chunk
                    int z = town.getHomeBlock().getZ() * 16 + 8;
                    location = new Location(world, x, world.getHighestBlockYAt(x, z), z);
                } else {
                    // Skip towns without spawn or homeblock
                    continue;
                }

                String markerId = Marker.generateId("town");
                Marker marker = new Marker(
                    markerId,
                    MarkerType.TOWN.getConfigKey(),
                    location,
                    town.getName()
                );

                // Set marker appearance - use banner for towns
                marker.setCursorType(MapCursor.Type.BANNER_GREEN);

                plugin.getMarkerManager().addMarker(marker);
                townCount++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to add marker for town " + town.getName() + ": " + e.getMessage());
            }
        }

        if (townCount > 0) {
            plugin.getLogger().fine("Updated " + townCount + " Towny town markers");
        }
    }

    private void updateNationMarkers() {
        if (!plugin.getConfig().getBoolean("markers.nations.enabled", true)) {
            return;
        }

        // Clear existing nation markers
        plugin.getMarkerManager().clearMarkersByType(MarkerType.NATION.getConfigKey());

        int maxNations = plugin.getConfig().getInt("markers.nations.max-nations", 50);
        boolean showCapital = plugin.getConfig().getBoolean("markers.nations.show-capital", true);
        int nationCount = 0;

        TownyAPI api = TownyAPI.getInstance();

        for (Nation nation : api.getNations()) {
            if (nationCount >= maxNations) {
                break;
            }

            try {
                if (!nation.hasCapital()) {
                    continue;
                }

                Town capital = nation.getCapital();
                Location location;

                if (showCapital && capital.hasSpawn()) {
                    // Use capital spawn point
                    location = capital.getSpawn();
                } else if (capital.hasHomeBlock()) {
                    // Use capital homeblock - convert TownBlock to Location
                    org.bukkit.World world = capital.getHomeBlock().getWorld().getBukkitWorld();
                    int x = capital.getHomeBlock().getX() * 16 + 8; // Center of chunk
                    int z = capital.getHomeBlock().getZ() * 16 + 8;
                    location = new Location(world, x, world.getHighestBlockYAt(x, z), z);
                } else {
                    continue;
                }

                String markerId = Marker.generateId("nation");
                Marker marker = new Marker(
                    markerId,
                    MarkerType.NATION.getConfigKey(),
                    location,
                    nation.getName() + " (Capital: " + capital.getName() + ")"
                );

                // Set marker appearance - use banner for nations
                marker.setCursorType(MapCursor.Type.BANNER_BLUE);

                plugin.getMarkerManager().addMarker(marker);
                nationCount++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to add marker for nation " + nation.getName() + ": " + e.getMessage());
            }
        }

        if (nationCount > 0) {
            plugin.getLogger().fine("Updated " + nationCount + " Towny nation markers");
        }
    }
}
