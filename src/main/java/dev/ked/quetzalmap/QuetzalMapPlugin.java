package dev.ked.quetzalmap;

import dev.ked.quetzalmap.commands.MapCommand;
import dev.ked.quetzalmap.integration.IntegrationManager;
import dev.ked.quetzalmap.listener.PlayerListener;
import dev.ked.quetzalmap.manager.MapManager;
import dev.ked.quetzalmap.manager.MarkerManager;
import dev.ked.quetzalmap.manager.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class QuetzalMapPlugin extends JavaPlugin {

    private static QuetzalMapPlugin instance;

    private MapManager mapManager;
    private MarkerManager markerManager;
    private PlayerDataManager playerDataManager;
    private IntegrationManager integrationManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        this.playerDataManager = new PlayerDataManager(this);
        this.markerManager = new MarkerManager(this);
        this.mapManager = new MapManager(this);
        this.integrationManager = new IntegrationManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register commands
        MapCommand mapCommand = new MapCommand(this);
        getCommand("qmap").setExecutor(mapCommand);
        getCommand("qmap").setTabCompleter(mapCommand);

        // Initialize integrations
        integrationManager.loadIntegrations();

        getLogger().info("QuetzalMap has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cleanup
        if (mapManager != null) {
            mapManager.shutdown();
        }

        if (integrationManager != null) {
            integrationManager.unloadIntegrations();
        }

        getLogger().info("QuetzalMap has been disabled!");
    }

    public static QuetzalMapPlugin getInstance() {
        return instance;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public MarkerManager getMarkerManager() {
        return markerManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }
}
