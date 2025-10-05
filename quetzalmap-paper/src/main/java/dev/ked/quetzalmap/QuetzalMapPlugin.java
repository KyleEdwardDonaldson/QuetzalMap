package dev.ked.quetzalmap;

import dev.ked.quetzalmap.server.WebServer;
import dev.ked.quetzalmap.web.pregen.TilePreGenerator;
import dev.ked.quetzalmap.web.tiles.TileManager;
import dev.ked.quetzalmap.web.world.WorldAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Main plugin class for QuetzalMap Paper integration.
 * Handles tile rendering, chunk tracking, and event-driven updates.
 */
public final class QuetzalMapPlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger(QuetzalMapPlugin.class.getName());

    private TileManager tileManager;
    private WorldAdapter worldAdapter;
    private WebServer webServer;
    private BatchUpdateScheduler updateScheduler;
    private ChunkEventListener chunkListener;
    private PlayerJoinListener playerJoinListener;
    private PlayerQuitListener playerQuitListener;
    private TilePreGenerator preGenerator;
    private PlayerTracker playerTracker;

    @Override
    public void onEnable() {
        LOGGER.info("QuetzalMap enabling...");

        try {
            // Initialize components
            initializeComponents();

            // Register event listeners
            registerListeners();

            // Start update scheduler
            updateScheduler.start();

            // Start web server
            webServer.start();

            // Start player tracker
            playerTracker.start();

            // Set up SSE callback to send initial player list on new connections
            webServer.getSSEManager().setOnNewConnection(() -> {
                LOGGER.fine("New SSE connection - sending initial player list");
                playerTracker.sendInitialPlayerList();
            });

            // Start pre-generation after a delay (let server finish starting)
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::startPreGeneration, 200L); // 10 seconds

            LOGGER.info("QuetzalMap enabled successfully!");
            LOGGER.info("Tiles directory: " + getTilesDirectory());
            LOGGER.info("Worlds directory: " + getWorldsDirectory());
            LOGGER.info("Web server: http://" + webServer.getHost() + ":" + webServer.getPort());

        } catch (Exception e) {
            LOGGER.severe("Failed to enable QuetzalMap: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        LOGGER.info("QuetzalMap disabling...");

        try {
            // Stop player tracker first
            if (playerTracker != null) {
                playerTracker.cancel();
            }

            // Stop pre-generation
            if (preGenerator != null) {
                preGenerator.stop();
            }

            // Stop web server
            if (webServer != null) {
                webServer.stop();
            }

            // Stop scheduler
            if (updateScheduler != null) {
                updateScheduler.stop();
            }

            // Shutdown tile manager
            if (tileManager != null) {
                tileManager.shutdown();
            }

            // Clear world adapter cache
            if (worldAdapter != null) {
                worldAdapter.clearAll();
            }

            LOGGER.info("QuetzalMap disabled successfully!");

        } catch (Exception e) {
            LOGGER.severe("Error during QuetzalMap shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize all plugin components.
     */
    private void initializeComponents() {
        // Get server directories
        Path tilesDir = getTilesDirectory();
        Path worldsDir = getWorldsDirectory();

        // Create tile manager
        tileManager = new TileManager(tilesDir);
        LOGGER.info("TileManager initialized");

        // Create world adapter
        worldAdapter = new WorldAdapter(worldsDir);
        LOGGER.info("WorldAdapter initialized");

        // Create web server
        String host = "0.0.0.0";  // TODO: Make configurable
        int port = 8123;           // Using existing Pterodactyl port allocation
        webServer = new WebServer(host, port, tileManager, tilesDir, worldsDir);
        LOGGER.info("WebServer initialized");

        // Create update scheduler with SSE manager
        updateScheduler = new BatchUpdateScheduler(this, tileManager, worldAdapter, webServer.getSSEManager());
        LOGGER.info("BatchUpdateScheduler initialized");

        // Create chunk listener
        chunkListener = new ChunkEventListener(tileManager, updateScheduler);
        LOGGER.info("ChunkEventListener initialized");

        // Create player tracker
        playerTracker = new PlayerTracker(this, webServer.getSSEManager());
        LOGGER.info("PlayerTracker initialized");

        // Create player event listeners
        playerJoinListener = new PlayerJoinListener(playerTracker);
        LOGGER.info("PlayerJoinListener initialized");

        playerQuitListener = new PlayerQuitListener(playerTracker);
        LOGGER.info("PlayerQuitListener initialized");
    }

    /**
     * Register event listeners.
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(chunkListener, this);
        Bukkit.getPluginManager().registerEvents(playerJoinListener, this);
        Bukkit.getPluginManager().registerEvents(playerQuitListener, this);
        LOGGER.info("Event listeners registered");
    }

    /**
     * Get the tiles directory (where rendered PNG tiles are stored).
     */
    private Path getTilesDirectory() {
        return getDataFolder().toPath().resolve("tiles");
    }

    /**
     * Get the worlds directory (where Minecraft world data is stored).
     */
    private Path getWorldsDirectory() {
        // Paper stores worlds in the server root directory
        return getServer().getWorldContainer().toPath();
    }

    /**
     * Get the tile manager instance.
     */
    public TileManager getTileManager() {
        return tileManager;
    }

    /**
     * Get the world adapter instance.
     */
    public WorldAdapter getWorldAdapter() {
        return worldAdapter;
    }

    /**
     * Get the update scheduler instance.
     */
    public BatchUpdateScheduler getUpdateScheduler() {
        return updateScheduler;
    }

    /**
     * Get the web server instance.
     */
    public WebServer getWebServer() {
        return webServer;
    }

    /**
     * Get the pre-generator instance.
     */
    public TilePreGenerator getPreGenerator() {
        return preGenerator;
    }

    /**
     * Start pre-generation for spawn area.
     * Called automatically on server start with a delay.
     */
    private void startPreGeneration() {
        try {
            // TODO: Make these configurable
            int pregenThreads = 4; // Low priority background threads
            int radiusTiles = 10;  // 10 tiles = 5120 blocks radius from spawn

            preGenerator = new TilePreGenerator(tileManager, pregenThreads);

            // Pre-generate spawn area for overworld
            Path worldsDir = getWorldsDirectory();
            preGenerator.preGenerateArea(worldsDir, "world", 0, 0, radiusTiles)
                    .thenAccept(stats -> {
                        LOGGER.info("Spawn area pre-generation complete: " + stats);
                    })
                    .exceptionally(error -> {
                        LOGGER.warning("Pre-generation failed: " + error.getMessage());
                        return null;
                    });

            LOGGER.info("Started background pre-generation for spawn area (radius=" + radiusTiles + " tiles)");

        } catch (Exception e) {
            LOGGER.warning("Failed to start pre-generation: " + e.getMessage());
        }
    }
}
