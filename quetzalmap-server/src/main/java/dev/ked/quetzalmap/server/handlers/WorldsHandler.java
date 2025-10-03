package dev.ked.quetzalmap.server.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Handler for retrieving available worlds with rendered tiles.
 */
public class WorldsHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(WorldsHandler.class.getName());

    private final Path worldsDirectory;

    public WorldsHandler(Path worldsDirectory) {
        this.worldsDirectory = worldsDirectory;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        List<String> availableWorlds = getAvailableWorlds();

        // Build JSON response
        StringBuilder json = new StringBuilder("{\"worlds\":[");
        for (int i = 0; i < availableWorlds.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(availableWorlds.get(i)).append("\"");
        }
        json.append("]}");

        exchange.setStatusCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(json.toString());
    }

    /**
     * Get list of worlds that have region files (i.e., have been explored).
     */
    private List<String> getAvailableWorlds() {
        List<String> worlds = new ArrayList<>();

        // Check common world directories
        String[] possibleWorlds = {"world", "world_nether", "world_the_end"};

        for (String worldName : possibleWorlds) {
            Path worldPath = worldsDirectory.resolve(worldName);
            Path regionPath = worldPath.resolve("region");

            if (Files.exists(regionPath) && Files.isDirectory(regionPath)) {
                // Check if there are any .mca files (region files)
                try (Stream<Path> files = Files.list(regionPath)) {
                    boolean hasRegionFiles = files.anyMatch(path ->
                        path.getFileName().toString().endsWith(".mca")
                    );

                    if (hasRegionFiles) {
                        worlds.add(worldName);
                        LOGGER.info("Found world with regions: " + worldName);
                    }
                } catch (IOException e) {
                    LOGGER.warning("Error checking world " + worldName + ": " + e.getMessage());
                }
            }
        }

        // Always include at least one world (default to "world")
        if (worlds.isEmpty()) {
            worlds.add("world");
        }

        return worlds;
    }
}
