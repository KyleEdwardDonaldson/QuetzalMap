package dev.ked.quetzalmap.integration;

import dev.ked.quetzalmap.QuetzalMapPlugin;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class IntegrationManager {

    private final QuetzalMapPlugin plugin;
    private final List<Integration> integrations;

    public IntegrationManager(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
        this.integrations = new ArrayList<>();
    }

    public void loadIntegrations() {
        // Stormcraft Integration
        if (isPluginEnabled("Stormcraft") && plugin.getConfig().getBoolean("integrations.stormcraft.enabled", true)) {
            StormcraftIntegration stormIntegration = new StormcraftIntegration(plugin);
            if (stormIntegration.initialize()) {
                integrations.add(stormIntegration);
                plugin.getLogger().info("Stormcraft integration enabled!");
            }
        }

        // Bazaar Integration
        if (isPluginEnabled("Bazaar") && plugin.getConfig().getBoolean("integrations.bazaar.enabled", true)) {
            BazaarIntegration shopIntegration = new BazaarIntegration(plugin);
            if (shopIntegration.initialize()) {
                integrations.add(shopIntegration);
                plugin.getLogger().info("Bazaar integration enabled!");
            }
        }

        // Towny Integration
        if (isPluginEnabled("Towny") && plugin.getConfig().getBoolean("integrations.towny.enabled", true)) {
            TownyIntegration townyIntegration = new TownyIntegration(plugin);
            if (townyIntegration.initialize()) {
                integrations.add(townyIntegration);
                plugin.getLogger().info("Towny integration enabled!");
            }
        }

        // Add more integrations here as they are developed
        // SilkRoad, StormcraftEvents, StormcraftDungeons, etc.
    }

    public void unloadIntegrations() {
        for (Integration integration : integrations) {
            integration.shutdown();
        }
        integrations.clear();
    }

    private boolean isPluginEnabled(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null &&
               Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    public List<Integration> getIntegrations() {
        return new ArrayList<>(integrations);
    }
}
