package dev.ked.quetzalmap.integration;

import dev.ked.bettershop.BetterShopPlugin;
import dev.ked.bettershop.shop.Listing;
import dev.ked.bettershop.shop.ListingType;
import dev.ked.bettershop.shop.ShopEntity;
import dev.ked.bettershop.shop.ShopRegistry;
import dev.ked.quetzalmap.QuetzalMapPlugin;
import dev.ked.quetzalmap.model.Marker;
import dev.ked.quetzalmap.model.MarkerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.map.MapCursor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class BetterShopIntegration implements Integration {

    private final QuetzalMapPlugin plugin;
    private BetterShopPlugin betterShopPlugin;
    private BukkitTask updateTask;
    private boolean initialized = false;

    public BetterShopIntegration(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean initialize() {
        try {
            // Get BetterShop plugin instance
            Plugin betterShopBukkit = Bukkit.getPluginManager().getPlugin("BetterShop");
            if (betterShopBukkit == null || !(betterShopBukkit instanceof BetterShopPlugin)) {
                plugin.getLogger().warning("BetterShop plugin not found");
                return false;
            }

            betterShopPlugin = (BetterShopPlugin) betterShopBukkit;

            // Start periodic marker updates
            int updateInterval = plugin.getConfig().getInt("integrations.bettershop.update-interval", 200);
            updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateMarkers, 20L, updateInterval);

            initialized = true;
            plugin.getLogger().info("BetterShop integration initialized");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize BetterShop integration: " + e.getMessage());
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

        // Clear all shop markers
        plugin.getMarkerManager().clearMarkersByType(MarkerType.SHOP.getConfigKey());
        initialized = false;
    }

    @Override
    public String getName() {
        return "BetterShop";
    }

    @Override
    public void updateMarkers() {
        if (!initialized || !plugin.getConfig().getBoolean("markers.shops.enabled", true)) {
            return;
        }

        // Clear existing shop markers
        plugin.getMarkerManager().clearMarkersByType(MarkerType.SHOP.getConfigKey());

        // Get shop registry
        ShopRegistry registry = betterShopPlugin.getShopRegistry();
        if (registry == null) {
            return;
        }

        // Get configuration
        int maxShops = plugin.getConfig().getInt("markers.shops.max-shops", 100);
        boolean groupByOwner = plugin.getConfig().getBoolean("markers.shops.group-by-owner", false);

        int shopCount = 0;

        // Get all shops (ShopEntity) from the registry
        Collection<ShopEntity> allShops = registry.getAllShops();

        // Track owners if grouping is enabled
        java.util.Set<UUID> processedOwners = new java.util.HashSet<>();

        for (ShopEntity shop : allShops) {
            if (shopCount >= maxShops) {
                break;
            }

            // If grouping by owner, only show one marker per owner
            if (groupByOwner) {
                if (processedOwners.contains(shop.getOwner())) {
                    continue;
                }
                processedOwners.add(shop.getOwner());
            }

            // Get listings for this shop to find a location
            List<Listing> shopListings = registry.getListingsByShop(shop.getId());
            if (shopListings.isEmpty()) {
                continue; // Shop has no listings (chests)
            }

            // Use the first listing's location as the shop marker location
            // Or could use creationLocation from ShopEntity
            Location shopLocation = shop.getCreationLocation();
            if (shopLocation == null) {
                // Fallback to first listing location
                shopLocation = shopListings.get(0).getLocation();
                if (shopLocation == null) {
                    continue;
                }
            }

            String markerId = Marker.generateId("shop");
            String shopName = shop.getName();

            Marker marker = new Marker(
                markerId,
                MarkerType.SHOP.getConfigKey(),
                shopLocation,
                shopName
            );

            // Store shop data in marker for potential future use
            marker.setData("owner", shop.getOwner());
            marker.setData("shopId", shop.getId());
            marker.setData("listingCount", shop.getListingCount());

            // Determine dominant listing type for cursor color
            // Count listing types to determine the shop's primary function
            int buyCount = 0;
            int sellCount = 0;
            for (Listing listing : shopListings) {
                if (listing.getType() == ListingType.BUY) {
                    buyCount++;
                } else if (listing.getType() == ListingType.SELL) {
                    sellCount++;
                }
            }

            // Set cursor type based on dominant listing type
            if (buyCount > sellCount) {
                marker.setCursorType(MapCursor.Type.BANNER_GREEN); // Players can buy here
            } else if (sellCount > buyCount) {
                marker.setCursorType(MapCursor.Type.BANNER_RED);   // Players can sell here
            } else {
                marker.setCursorType(MapCursor.Type.BANNER_YELLOW); // Mixed shop
            }

            plugin.getMarkerManager().addMarker(marker);
            shopCount++;
        }

        if (shopCount > 0) {
            plugin.getLogger().fine("Updated " + shopCount + " BetterShop markers");
        }
    }
}