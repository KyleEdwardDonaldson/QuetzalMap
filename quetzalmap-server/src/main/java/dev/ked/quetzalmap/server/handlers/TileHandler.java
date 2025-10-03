package dev.ked.quetzalmap.server.handlers;

import dev.ked.quetzalmap.web.tiles.Tile;
import dev.ked.quetzalmap.web.tiles.TileCoord;
import dev.ked.quetzalmap.web.tiles.TileManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP handler for serving map tiles.
 * Supports URLs like: /tiles/{world}/{zoom}/{x}_{z}.png
 */
public final class TileHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(TileHandler.class.getName());
    private static final Pattern TILE_PATTERN = Pattern.compile("^/tiles/([^/]+)/(\\d+)/([-\\d]+)_([-\\d]+)\\.png$");

    private final TileManager tileManager;
    private final Path tilesDirectory;
    private final Path worldsDirectory;

    public TileHandler(TileManager tileManager, Path tilesDirectory, Path worldsDirectory) {
        this.tileManager = tileManager;
        this.tilesDirectory = tilesDirectory;
        this.worldsDirectory = worldsDirectory;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        String path = exchange.getRequestPath();
        Matcher matcher = TILE_PATTERN.matcher(path);

        if (!matcher.matches()) {
            sendError(exchange, StatusCodes.BAD_REQUEST, "Invalid tile path format");
            return;
        }

        try {
            String world = matcher.group(1);
            int zoom = Integer.parseInt(matcher.group(2));
            int x = Integer.parseInt(matcher.group(3));
            int z = Integer.parseInt(matcher.group(4));

            TileCoord coord = new TileCoord(world, zoom, x, z);
            LOGGER.info("*** TILE REQUEST: " + coord + " ***");

            // Check if tile exists on disk
            Path tilePath = tilesDirectory.resolve(coord.getRelativePath());

            if (Files.exists(tilePath)) {
                LOGGER.info("Tile exists on disk at: " + tilePath + " - serving from cache");
                // Serve from disk
                serveTileFromDisk(exchange, tilePath);
            } else {
                LOGGER.info("Tile does NOT exist on disk - rendering new tile");
                // Tile doesn't exist - need to render
                Path worldDir = worldsDirectory.resolve(coord.getWorld());
                renderAndServeTile(exchange, coord, worldDir);
            }

        } catch (NumberFormatException e) {
            sendError(exchange, StatusCodes.BAD_REQUEST, "Invalid tile coordinates");
        } catch (Exception e) {
            LOGGER.severe("Error serving tile: " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Serve a tile from disk.
     */
    private void serveTileFromDisk(HttpServerExchange exchange, Path tilePath) {
        try {
            byte[] tileData = Files.readAllBytes(tilePath);

            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "image/png");
            exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, tileData.length);
            exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "public, max-age=300"); // 5 min cache

            exchange.startBlocking();
            exchange.getOutputStream().write(tileData);
            exchange.getOutputStream().close();

        } catch (IOException e) {
            LOGGER.warning("Failed to read tile from disk: " + tilePath);
            sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Failed to read tile");
        }
    }

    /**
     * Render a tile on-demand and serve it.
     */
    private void renderAndServeTile(HttpServerExchange exchange, TileCoord coord, Path worldDir) {
        exchange.dispatch(() -> {
            try {
                LOGGER.info("Rendering tile for world directory: " + worldDir.toAbsolutePath());

                // Render tile asynchronously
                CompletableFuture<Tile> future = tileManager.renderTile(coord, worldDir);

                future.thenAccept(tile -> {
                    // Tile rendered - serve from disk
                    Path tilePath = tilesDirectory.resolve(coord.getRelativePath());
                    if (Files.exists(tilePath)) {
                        serveTileFromDisk(exchange, tilePath);
                    } else {
                        sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Tile render failed");
                    }
                }).exceptionally(e -> {
                    LOGGER.severe("Failed to render tile " + coord + ": " + e.getMessage());
                    sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Tile render error");
                    return null;
                });

            } catch (Exception e) {
                LOGGER.severe("Error rendering tile: " + e.getMessage());
                sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR, "Render error");
            }
        });
    }

    /**
     * Send an error response.
     */
    private void sendError(HttpServerExchange exchange, int statusCode, String message) {
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send(message);
    }
}
